/*
 * Copyright (C) 2022 jlgranda
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jlgranda.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jlgranda
 */
public class IOUtils {
 
    public static boolean prepareDirectory(String qgzFilePath) {
        //Preparar el directorio del proyecto
        Path path = Paths.get(qgzFilePath);
        if (Files.notExists(path, LinkOption.NOFOLLOW_LINKS)) {
            try {
                //Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxrwx");                
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrw-r-x");                
                Files.createDirectories(path, PosixFilePermissions.asFileAttribute(perms));
                
                //outputLS(path);         
                //https://stackoverflow.com/questions/41877638/java-unable-to-create-directory-with-777-permission-has-775-instead
                Files.setPosixFilePermissions(path, perms);            
                //outputLS(path); 
                
            } catch (IOException ex) {
                Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, null, ex);
                return false; 
            }
        }
        return true;
    }
    
    public static boolean chmod777(String path){
        
        try {
            chmod(path, "rwxrwxrwx");
            return true;
        } catch (IOException ex) {
            Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    public static boolean chmod765(String path){
        
        try {
            chmod(path, "rwxrw-r-x");
            return true;
        } catch (IOException ex) {
            Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    public static void chmod(String path, String permissions) throws IOException {
        Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, "chmod recursivo en la ruta {0}...", path);
        chmod(Paths.get(path), permissions);
    }
    private static void chmod(Path path, String permissions) throws IOException {
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString(permissions);        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    chmod(entry, permissions);
                }
                Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, "Estableciendo permison permissions a la ruta {0}...", entry.toString());
                Files.setPosixFilePermissions(entry, perms);   
                Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, "Establecdo permison permissions a la ruta {0}", entry.toString());
            }
        }
    }
}
