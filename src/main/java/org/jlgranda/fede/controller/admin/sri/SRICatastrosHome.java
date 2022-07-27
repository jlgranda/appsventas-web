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

import com.jlgranda.fede.ejb.sri.SRICatastrosEmpresaFantasmaService;
import com.jlgranda.fede.ejb.sri.SRICatastrosRimpeService;
import com.jlgranda.fede.ejb.sri.SRICatastrosRucService;
import org.jlgranda.fede.controller.FedeController;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.io.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.sri.SRICatastrosRuc;
import org.jlgranda.fede.model.sri.SRICatastrosRimpe;
import org.jlgranda.fede.model.sri.SRICatastrosEmpresaFantasma;
import org.jpapi.model.Group;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.jlgranda.fede.model.sales.SRICatastrosType;
import org.jpapi.util.Strings;

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

    private String nombreDeArchivo;
    Logger logger = LoggerFactory.getLogger(SRICatastrosHome.class);

    @EJB
    private SRICatastrosRucService sriCatastrosRucService; // se llama el servicio de certificacion
    private SRICatastrosRimpeService sriCatastrosRimpeService; // se llama el servicio de certificacion
    private SRICatastrosEmpresaFantasmaService sriCatastrosEmpreFantasmaService; // se llama el servicio de certificacion

    private SRICatastrosType tipoSRICatastro;

    @Inject
    private SettingHome settingHome;

    public String getNombreDeArchivo() {
        return nombreDeArchivo;
    }

    public void setNombreDeArchivo(String nombreDeArchivo) {
        this.nombreDeArchivo = nombreDeArchivo;
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
        tipoSRICatastro = SRICatastrosType.RUC;
        setOutcome("sricatastros");
    }

    public SRICatastrosType getTipoSRICatastro() {
        return tipoSRICatastro;
    }

    public void setTipoSRICatastro(SRICatastrosType tipoSRICatastro) {
        this.tipoSRICatastro = tipoSRICatastro;
    }

    public boolean isSugerenciasEncontradas() {
        return sugerenciasEncontradas;
    }

    public void setSugerenciasEncontradas(boolean sugerenciasEncontradas) {
        this.sugerenciasEncontradas = sugerenciasEncontradas;
    }

    public void guardarCatastro() throws IOException, ParseException {
        if (null == tipoSRICatastro) {
            this.addWarningMessage("Seleccione algun tipo de catastro", "");
        } else switch (tipoSRICatastro) {
            case RUC:
                guardarCatastroRUC();
                break;
            case RIMPE:
                guardarCatastroRIMPE();
                break;        
            case EMPRESA_FANTASMA:
                guardarCatastroEmpreFantasma();
                break;
            default:
                this.addWarningMessage("Seleccione algun tipo de catastro", "");
                break;
        }
    }

    private void guardarCatastroRUC() throws IOException, ParseException {
        if (Strings.isNullOrEmpty(getNombreDeArchivo())) {
            this.addWarningMessage("Ingrese archivo compromido de CATASTROS RUC", "");
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

    private void guardarCatastroRIMPE() throws IOException {

        if (Strings.isNullOrEmpty(getNombreDeArchivo())) {
            this.addWarningMessage("Ingrese archivo excel de CATASTROS RIMPE", "");
        } else {

            List<SRICatastrosRimpe> catastros = new ArrayList<>();
//        try {
            FileInputStream file = new FileInputStream(new File(directorioZip + getNombreDeArchivo()));
            XSSFWorkbook wb = new XSSFWorkbook(file);
            XSSFSheet sheet = wb.getSheetAt(0);
            int numFilas = sheet.getLastRowNum();
            for (int i = 2; i <= numFilas; i++) {
                Row fila = sheet.getRow(i);
                SRICatastrosRimpe objCatastrosRimpe = sriCatastrosRimpeService.createInstance();
                objCatastrosRimpe.setNumeroRuc(fila.getCell(0).getStringCellValue());
                objCatastrosRimpe.setRazonSocial(fila.getCell(1).getStringCellValue());
                objCatastrosRimpe.setZona(fila.getCell(2).getStringCellValue());
                objCatastrosRimpe.setRegimen(fila.getCell(3).getStringCellValue());
                objCatastrosRimpe.setNegocioPopular(fila.getCell(4).getStringCellValue());
                catastros.add(objCatastrosRimpe);
            }
            file.close();
            //Guardar la lista de instancias
            sriCatastrosRimpeService.bulkSave(catastros);
            this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), "Se importó el catastro de RIMPE. Cantidad de registros " + catastros.size());
//        } catch (Exception e) {
//        }
        }
    }

    private void guardarCatastroEmpreFantasma() throws IOException, ParseException {

        if (Strings.isNullOrEmpty(getNombreDeArchivo())) {
            this.addWarningMessage("Ingrese archivo excel de CATASTROS EMPRESAS FANTASMAS", "");
        } else {
            List<SRICatastrosEmpresaFantasma> catastros = new ArrayList<>();
            try {
                FileInputStream file = new FileInputStream(new File(directorioZip + getNombreDeArchivo()));
                XSSFWorkbook wb = new XSSFWorkbook(file);
                XSSFSheet sheet = wb.getSheetAt(0);
                int numFilas = sheet.getLastRowNum();
                for (int i = 4; i <= numFilas; i++) {
                    Row fila = sheet.getRow(i);
                    SRICatastrosEmpresaFantasma objCatastros = sriCatastrosEmpreFantasmaService.createInstance();
                    objCatastros.setNumero(fila.getCell(0).getStringCellValue());
                    objCatastros.setNumero(fila.getCell(1).getStringCellValue());
                    objCatastros.setRazonSocial(fila.getCell(2).getStringCellValue());
                    objCatastros.setTipoContribuyente(fila.getCell(3).getStringCellValue());
                    objCatastros.setZona(fila.getCell(4).getStringCellValue());
                    objCatastros.setProvincia(fila.getCell(5).getStringCellValue());
                    objCatastros.setOficio(fila.getCell(6).getStringCellValue());
                    objCatastros.setFechaNotificacion1(Dates.toDate(fila.getCell(7).getStringCellValue(), settingHome.getValue("fede.name.pattern", "dd/MM/yyyy")));
                    objCatastros.setNroResolucion(fila.getCell(8).getStringCellValue());
                    objCatastros.setFechaNotificacion2(Dates.toDate(fila.getCell(9).getStringCellValue(), settingHome.getValue("fede.name.pattern", "dd/MM/yyyy")));
                    objCatastros.setEstadoRuc(fila.getCell(10).getStringCellValue());
                    objCatastros.setFechaInicioCalificacion(Dates.toDate(fila.getCell(11).getStringCellValue(), settingHome.getValue("fede.name.pattern", "dd/MM/yyyy")));
                    objCatastros.setFechaFinCalificacion(Dates.toDate(fila.getCell(12).getStringCellValue(), settingHome.getValue("fede.name.pattern", "dd/MM/yyyy")));
                    objCatastros.setResolucion(fila.getCell(13).getStringCellValue());
                    objCatastros.setFechaNotificacionResolucion(Dates.toDate(fila.getCell(14).getStringCellValue(), settingHome.getValue("fede.name.pattern", "dd/MM/yyyy")));
                    objCatastros.setNumOficioReactivacion(fila.getCell(15).getStringCellValue());
                    objCatastros.setFechaNotificacion3(Dates.toDate(fila.getCell(16).getStringCellValue(), settingHome.getValue("fede.name.pattern", "dd/MM/yyyy")));
                    objCatastros.setEstado(fila.getCell(17).getStringCellValue());
                    objCatastros.setFechaReactivacion(Dates.toDate(fila.getCell(18).getStringCellValue(), settingHome.getValue("fede.name.pattern", "dd/MM/yyyy")));
                    objCatastros.setInstanciaInpugna(fila.getCell(19).getStringCellValue());
                    objCatastros.setEstadoInpugna(fila.getCell(20).getStringCellValue());
                    catastros.add(objCatastros);
                }
                file.close();
                //Guardar la lista de instancias
                sriCatastrosEmpreFantasmaService.bulkSave(catastros);
                this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), "Se importó el catastro de EMPRESAS FANTASMAS. Cantidad de registros " + catastros.size());
            } catch (Exception e) {
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
                if (cont > 0) {
                    String[] parts = cadena.split("\t", -1);
                    SRICatastrosRuc objCatastros = sriCatastrosRucService.createInstance();
                    objCatastros.setNumeroRuc(parts[0]);
                    objCatastros.setRazonSocial(parts[1]);
                    objCatastros.setNombreComercia(parts[2]);
                    objCatastros.setEstadoContribuyente(parts[3]);
                    objCatastros.setClaseContribuyente(parts[4]);
                    objCatastros.setFechaInicioActividades(Dates.toDate(parts[5], settingHome.getValue("fede.name.pattern", "dd/MM/yyyy")));
                    objCatastros.setFechaActualizacion(Dates.toDate(parts[6], settingHome.getValue("fede.name.pattern", "dd/MM/yyyy")));
                    objCatastros.setFechaSuspencionDefinitiva(Dates.toDate(parts[7], settingHome.getValue("fede.name.pattern", "dd/MM/yyyy")));
                    objCatastros.setFecharReinicioActividades(Dates.toDate(parts[8], settingHome.getValue("fede.name.pattern", "dd/MM/yyyy")));
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

    public static Date ParseFecha(String fecha) {
        Date fechaDate = null;
        if (!fecha.equals("")) {
            SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy");
            try {
                fechaDate = formato.parse(fecha);
            } catch (java.text.ParseException ex) {
                java.util.logging.Logger.getLogger(SRICatastrosHome.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return fechaDate;
    }

    public void handleFileUpload(FileUploadEvent event) {
        
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        System.out.println("handleFileUpload ");
        System.out.println("event.getFile().getFileName(): " + event.getFile().getFileName());
        
        setNombreDeArchivo(event.getFile().getFileName());
        setHandledFileUpload(true);
        try {
            File carpetaRaiz = new File(directorioZip);
            File archivoTemporal;
            for (String archivo : carpetaRaiz.list()) {
                archivoTemporal = new File(carpetaRaiz + File.separator + archivo);
                archivoTemporal.delete();
                archivoTemporal.deleteOnExit();
            }

            byte[] fileByte = IOUtils.toByteArray(event.getFile().getInputStream());

            try ( FileOutputStream fos = new FileOutputStream(directorioZip + getNombreDeArchivo())) {
                fos.write(fileByte);
            }
            
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Cargado en: " + directorioZip + getNombreDeArchivo());
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