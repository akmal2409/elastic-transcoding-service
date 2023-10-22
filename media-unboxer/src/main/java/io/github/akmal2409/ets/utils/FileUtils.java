package io.github.akmal2409.ets.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

public final class FileUtils {

  private FileUtils() {
    throw new IllegalStateException("Cannot instantiate a utility class");
  }


  public static String stripExtension(String name) {
    final int lastDotIndex = name.lastIndexOf('.');

    if (lastDotIndex == -1 || lastDotIndex == 0 || lastDotIndex == name.length() - 1) return name;

    return name.substring(0, lastDotIndex);
  }

  public static void deleteDirectory(Path directory) throws IOException {
    Files.walkFileTree(directory, Set.of(), 10, new FileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
          throws IOException {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        // since we delete only files on the way, we need to ensure that after we are done
        // visiting, we also delete the folder
        Files.delete(dir); // folder must be empty
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
