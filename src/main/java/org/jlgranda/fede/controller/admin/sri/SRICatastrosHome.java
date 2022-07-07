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
package org.jlgranda.fede.controller.admin.sri;

import com.jlgranda.fede.ejb.sri.SRICatastrosRucService;
import org.jlgranda.fede.controller.FedeController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.io.IOUtils;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.sri.SRICatastrosRuc;
import org.jpapi.model.Group;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Strings;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author jlgranda
 */
@Named(value = "catastrosHome")
@ViewScoped
public class SRICatastrosHome extends FedeController implements Serializable {
    private static final long serialVersionUID = -1007161141552849702L;
    
    private String directorioZip = "";

    private boolean handledFileUpload;

    private byte[] fileByte;
    private File file;
    private String nomfile;
    Logger logger = LoggerFactory.getLogger(SRICatastrosHome.class);

    @EJB
    private SRICatastrosRucService sriCatastrosRucService; // se llama el servicio de certificacion
    
    private String tipoSRICatastro;

    @Inject
    private SettingHome settingHome;

    public String getNomFile() {
        return nomfile;
    }

    public void setNomFile(String file) {
        this.nomfile = file;
    }

    public byte[] getFileByte() {
        return fileByte;
    }

    public void setFileByte(byte[] file) {
        this.fileByte = file;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public boolean isHandledFileUpload() {
        return handledFileUpload;
    }

    public void setHandledFileUpload(boolean handledFileUpload) {
        this.handledFileUpload = handledFileUpload;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Bandera para indicar si encontró sugerencias en elementos autocomplete
     */
    private boolean sugerenciasEncontradas = false;

    @PostConstruct
    public void init() {
        this.directorioZip = settingHome.getValue("app.sri.directorio.catastros", "/tmp/");
        tipoSRICatastro = "RUC";
    }

    public boolean isSugerenciasEncontradas() {
        return sugerenciasEncontradas;
    }

    public void setSugerenciasEncontradas(boolean sugerenciasEncontradas) {
        this.sugerenciasEncontradas = sugerenciasEncontradas;
    }

    public void guardarCatastro () throws IOException, ParseException {
        if ("RUC".equalsIgnoreCase(tipoSRICatastro)){
            guardarCatastroRUC();
        }
    }
    private void guardarCatastroRUC() throws IOException, ParseException {
        if (getNomFile() == null) {
            this.addWarningMessage("Ingrese archivo compromido de catastros", "");
        } else {

            List<SRICatastrosRuc> catastros = new ArrayList<>();
            //ruta donde están los archivos .zip
            File carpetaExtraer = new File(directorioZip);
//            valida si existe el directorio
            if (carpetaExtraer.exists()) {
//            lista los archivos que hay dentro  del directorio
                File[] ficheros = carpetaExtraer.listFiles();
//            ciclo para recorrer todos los archivos .zip
                for (File fichero : ficheros) {
                    try {
                        //crea un buffer temporal para el archivo que se va descomprimir
                        ZipInputStream zis = new ZipInputStream(new FileInputStream(directorioZip + fichero.getName()));
                        ZipEntry salida;
                        //recorre todo el buffer extrayendo uno a uno cada archivo.zip y creándolos de nuevo en su archivo original 
                        while (null != (salida = zis.getNextEntry())) {
                            try (
                                     FileOutputStream fos = new FileOutputStream(directorioZip + salida.getName())) {
                                int leer;
                                byte[] buffer = new byte[1024];
                                while (0 < (leer = zis.read(buffer))) {
                                    fos.write(buffer, 0, leer);
                                }
                            }
                            zis.closeEntry();
                            
                            catastros = obtenerListadoInstanciasSRICatastroRuc(directorioZip + salida.getName());
                        }
                    } catch (FileNotFoundException e) {
                    } catch (IOException e) {
                    }
                }
                
                
                //Guardar la lista de instancias
                sriCatastrosRucService.bulkSave(catastros);
                
                this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), "Se importó el catastro de RUC. Cantidad de registros " + catastros.size());
            } else {
                System.out.println("No se encontró el directorio..");
            }
        }
    }

    private List<SRICatastrosRuc> obtenerListadoInstanciasSRICatastroRuc(String archivo) throws FileNotFoundException, IOException, ParseException {
        String cadena;
        List<SRICatastrosRuc> items = new ArrayList<>();
        FileReader f = new FileReader(archivo);
        int cont;
        try ( BufferedReader b = new BufferedReader(f)) {
            cont = 0;
            while ((cadena = b.readLine()) != null) {
                if (cont > 0 && cont < 1000) {
                    String[] parts = cadena.split("\t", -1);
                    SRICatastrosRuc objCatastros = new SRICatastrosRuc();
                    objCatastros.setNumeroRuc(parts[0]);
                    objCatastros.setRazonSocial(parts[1]);
                    objCatastros.setNombreComercia(parts[2]);
                    objCatastros.setEstadoContribuyente(parts[3]);
                    objCatastros.setClaseContribuyente(parts[4]);
                    objCatastros.setFechaInicioActividades(Dates.toDate(parts[5], settingHome.getValue("app.sri.directorio.catastros", "dd/MM/yyyy")));
                    objCatastros.setFechaActualizacion(Dates.toDate(parts[6], settingHome.getValue("app.sri.directorio.catastros", "dd/MM/yyyy")));
                    objCatastros.setFechaSuspencionDefinitiva(Dates.toDate(parts[7], settingHome.getValue("app.sri.directorio.catastros", "dd/MM/yyyy")));
                    objCatastros.setFecharReinicioActividades(Dates.toDate(parts[8], settingHome.getValue("app.sri.directorio.catastros", "dd/MM/yyyy")));
                    objCatastros.setObligado(parts[9]);
                    objCatastros.setTipoContribuyente(parts[10]);
                    objCatastros.setNumeroEstablecimiento(parts[11]);
                    objCatastros.setNombreFantasiaComercial(parts[12]);
                    objCatastros.setEstadoEstablecimiento(parts[13]);
                    objCatastros.setDescripcionProvincia(parts[14]);
                    objCatastros.setDescripcionCanton(parts[15]);
                    objCatastros.setDescripcionParroquia(parts[16]);
                    objCatastros.setCodigoCiu(parts[17]);
                    objCatastros.setActividadEconomica(parts[18]);
                    items.add(objCatastros);
                }

                cont++;
            }
        }
        return items;
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void handleFileUpload(FileUploadEvent event) {
        setNomFile(event.getFile().getFileName());
        setHandledFileUpload(true);
        try {
            File carpetaRaiz = new File(directorioZip);
            File archivoTemporal;
            for (String archivo : carpetaRaiz.list()) {
                archivoTemporal = new File(carpetaRaiz + File.separator + archivo);
                archivoTemporal.delete();
                archivoTemporal.deleteOnExit();
            }

            setFileByte(IOUtils.toByteArray(event.getFile().getInputStream()));

            try ( FileOutputStream fos = new FileOutputStream(directorioZip + getNomFile())) {
                fos.write(getFileByte());
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(SRICatastrosHome.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public List<Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
