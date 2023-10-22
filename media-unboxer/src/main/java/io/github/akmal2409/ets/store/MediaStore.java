package io.github.akmal2409.ets.store;

import io.github.akmal2409.ets.exception.DuplicateJobException;
import io.github.akmal2409.ets.exception.VideoDownloadException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

/**
 * Class containing required operations for carrying out a preprocessing job such as downloading the
 * source video file and uploading preprocessed files.
 */
public class MediaStore {

  private static final Logger log = LoggerFactory.getLogger(MediaStore.class);

  /**
   * Storage folder that keeps the source files. {videoFolder}/{jobId}/{filename}.{extension}
   */
  private final Path videoFolder;
  private final S3TransferManager s3TransferManager;

  public MediaStore(@NotNull Path videoFolder,
      @NotNull S3TransferManager s3TransferManager) {
    this.videoFolder = videoFolder;
    this.s3TransferManager = s3TransferManager;
  }


  /**
   * Downloads source video file to the folder on disk and returns the path to the file.
   *
   * @param jobId  of the transcoding job.
   * @param source e.g. s3://bucket/key
   * @return path to the file.
   * @throws VideoDownloadException if the download failed or preparation for download
   * @throws DuplicateJobException  if the contents cannot be stored because this job has associated
   *                                files on disk.
   */
  public Path downloadSource(@NotNull UUID jobId, @NotNull String source) {
    final var s3Source = S3Source.from(source);

    final String fileName = s3Source.getFileName();

    Path jobDirectory;
    log.debug("message=Preparing to download video;job_id={}bucket={};file={}", jobId, s3Source.getBucket(),
        s3Source.getBucket());
    try {
      jobDirectory = createJobDirectoryOrElseFail(jobId);
      log.debug("message=Created job directory {};jobId={}", jobDirectory, jobId);
    } catch (IOException e) {
      throw new VideoDownloadException("Cannot set up folder", e, jobId);
    }

    final var filePath = jobDirectory.resolve(fileName);

    final FileDownload download = s3TransferManager
                                      .downloadFile(
                                          DownloadFileRequest.builder()
                                              .getObjectRequest(b -> b.bucket(s3Source.getBucket()).key(s3Source.getKey()))
                                              .destination(filePath)
                                              .addTransferListener(
                                                  LoggingTransferListener.create())
                                              .build());

    try {
      download.completionFuture().join();
      log.debug("message=Downloaded file successfully;jobId={};bucket={};file={};location={}",
          jobId, s3Source.getBucket(), s3Source.getKey(), jobDirectory);

      return filePath;
    } catch (CancellationException e) {
      throw new VideoDownloadException("Download failed because it was cancelled", e, jobId);
    } catch (CompletionException e) {
      throw new VideoDownloadException("Download failed due to exception", e.getCause(), jobId);
    }
  }

  /**
   * Uploads directory with processed files such as segments, index file, audio etc. to the
   * destination bucket with a prefix. Files located at the top of the folder will have
   * {@code keyPrefix} all files in the nested directories will have a key: {@code keyPrefix} +
   * directories + fileName
   *
   * @param output protocol formatted string e.g. s3://bucket/keyPrefix
   * @param directory path to upload.
   */
  public void uploadProcessedFiles(String output,
      @NotNull Path directory) {
    S3Output s3Output = S3Output.from(output);

    log.debug(
        "message=Starting upload of processed files from directory {} to bucket {} with key {};bucket={}",
        directory, s3Output.getBucket(), s3Output.getKey(), s3Output.getBucket());

    final var uploadRequest = UploadDirectoryRequest.builder()
                                  .followSymbolicLinks(false)
                                  .maxDepth(10)
                                  .s3Prefix(s3Output.getKey())
                                  .source(directory)
                                  .bucket(s3Output.getBucket())
                                  .build();

    final DirectoryUpload upload = this.s3TransferManager.uploadDirectory(uploadRequest);

    try {
      upload.completionFuture().join();
      log.debug(
          "message=Finished upload of processed files from directory {} to bucket {} with key {};bucket={}",
          directory, s3Output.getBucket(), s3Output.getKey(), s3Output.getBucket());
    } catch (CompletionException e) {
      throw new ProcessedFilesUploadFailedException(directory, s3Output.getBucket(), s3Output.getBucket());
    }
  }

  private Path createJobDirectoryOrElseFail(UUID jobId) throws IOException {
    final var jobFileFolder = videoFolder.resolve(jobId.toString());

    synchronized (MediaStore.class) {
      if (Files.exists(jobFileFolder)) {
        log.error("message=Duplicate job detected;jobId={}", jobId);
        throw new DuplicateJobException(
            "Cannot create directory for a job because it already exists: " + jobFileFolder, jobId);
      } else {
        Files.createDirectories(jobFileFolder);
      }
    }

    return jobFileFolder;
  }
}
