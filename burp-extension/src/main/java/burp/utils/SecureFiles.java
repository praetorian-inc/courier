/*
 * Copyright Praetorian Security Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package burp.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class SecureFiles {
    private static final Set<java.nio.file.attribute.PosixFilePermission> DIRECTORY_PERMISSIONS =
            PosixFilePermissions.fromString("rwx------");
    private static final Set<java.nio.file.attribute.PosixFilePermission> FILE_PERMISSIONS =
            PosixFilePermissions.fromString("rw-------");

    private SecureFiles() {
    }

    public static Path createPrivateDirectories(Path directory) throws IOException {
        Files.createDirectories(directory);
        applyPermissions(directory, true);
        return directory;
    }

    public static BufferedWriter newPrivateAppendWriter(Path file, Charset charset)
            throws IOException {
        createPrivateDirectories(file.toAbsolutePath().getParent());
        createPrivateFile(file);
        return Files.newBufferedWriter(file, charset,
                StandardOpenOption.WRITE, StandardOpenOption.APPEND);
    }

    public static void writePrivateString(Path file, String content, Charset charset)
            throws IOException {
        createPrivateDirectories(file.toAbsolutePath().getParent());
        createPrivateFile(file);
        Files.writeString(file, content, charset,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        applyPermissions(file, false);
    }

    private static void createPrivateFile(Path file) throws IOException {
        if (!Files.exists(file)) {
            try {
                Files.createFile(file, PosixFilePermissions.asFileAttribute(FILE_PERMISSIONS));
            } catch (UnsupportedOperationException exception) {
                Files.createFile(file);
            }
        }
        applyPermissions(file, false);
    }

    private static void applyPermissions(Path path, boolean directory) throws IOException {
        try {
            Files.setPosixFilePermissions(path,
                    directory ? DIRECTORY_PERMISSIONS : FILE_PERMISSIONS);
        } catch (UnsupportedOperationException exception) {
            AclFileAttributeView aclView = Files.getFileAttributeView(path, AclFileAttributeView.class);
            if (aclView != null) {
                AclEntry ownerAccess = AclEntry.newBuilder()
                        .setType(AclEntryType.ALLOW)
                        .setPrincipal(Files.getOwner(path))
                        .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                        .build();
                aclView.setAcl(List.of(ownerAccess));
                return;
            }

            java.io.File file = path.toFile();
            boolean permissionsUpdated = file.setReadable(false, false);
            permissionsUpdated &= file.setWritable(false, false);
            permissionsUpdated &= file.setExecutable(false, false);
            permissionsUpdated &= file.setReadable(true, true);
            permissionsUpdated &= file.setWritable(true, true);
            if (directory) {
                permissionsUpdated &= file.setExecutable(true, true);
            }
            if (!permissionsUpdated) {
                throw new IOException("Unable to apply owner-only permissions to " + path);
            }
        }
    }
}
