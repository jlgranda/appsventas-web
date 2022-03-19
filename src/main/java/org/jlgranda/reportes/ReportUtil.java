/*
 * Licensed under the TECNOPRO License, Version 1.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.tecnopro.net/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jlgranda.reportes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jlgranda.fede.Constantes;
import org.jlgranda.util.IOUtils;

/**
 * The Class ReportUtil.
 *
 * @author jlgranda
 */
public class ReportUtil implements Serializable {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 8814862516698789539L;

    private static ReportUtil instance;
    
    private ReportUtil(){}
    
    public static synchronized ReportUtil getInstance(){
        if(instance == null){
            instance = new ReportUtil();
        }
        return instance;
    }

    /**
     * Generar reporte.
     *
     * @param rutaDirectorioSalida
     * @param rutaArchivoJasper
     * @param params the params
     * @return the string
     * @throws JRException the JR exception
     */
    public String generarReporte(String rutaDirectorioSalida, String rutaArchivoJasper, Map<String, Object> params) throws JRException {
        String nombreReporteSalida = "";
        
        Connection conn = null;
        OutputStream output = null;
        try {
            InitialContext initialContext = null;
            DataSource ds = null;

            initialContext = new InitialContext();

            ds = (DataSource) initialContext.lookup(Constantes.JNDI_NAME);
            conn = ds.getConnection();

            JasperReport jasperReport = null;

            jasperReport = (JasperReport) JRLoader.loadObjectFromFile(rutaArchivoJasper);
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport, params, conn);

            nombreReporteSalida = Constantes.REPORTE_BASE_NAME + System.currentTimeMillis() + ".pdf";
            //nombreReporteSalida = "kellyreport.pdf";

            String nombreDocumento = rutaDirectorioSalida + nombreReporteSalida;
            output = new FileOutputStream(
                    new File(nombreDocumento));
            JasperExportManager.exportReportToPdfStream(jasperPrint, output);

        } catch (NamingException | SQLException | JRException | IOException e) {
            Logger.getLogger(ReportUtil.class.getName()).log(Level.SEVERE, e.getLocalizedMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace(System.out);
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ex) {
                    ex.printStackTrace(System.out);
                }
            }
        }

        return nombreReporteSalida;
    }
    
    /**
     * Generar reporte.
     *
     * @param rutaDirectorioSalida
     * @param rutaArchivoJasper
     * @param nombreReporte
     * @param params the params
     * @return the string
     * @throws JRException the JR exception
     */
    public String generarReporte(String rutaDirectorioSalida, String rutaArchivoJasper, String nombreReporte, Map<String, Object> params) throws JRException {
        String nombreReporteSalida = "";
        
        //Aseguar directorio de salida de reportes
        IOUtils.prepareDirectory(rutaDirectorioSalida);
        
        Connection conn = null;
        OutputStream output = null;
        try {
            InitialContext initialContext = null;
            DataSource ds = null;

            initialContext = new InitialContext();

            ds = (DataSource) initialContext.lookup(Constantes.JNDI_NAME);
            conn = ds.getConnection();

            JasperReport jasperReport = null;

            jasperReport = (JasperReport) JRLoader.loadObjectFromFile(rutaArchivoJasper);
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport, params, conn);
            
            nombreReporteSalida = nombreReporte + ".pdf";
            //nombreReporteSalida = "kellyreport.pdf";

            String nombreDocumento = rutaDirectorioSalida + nombreReporteSalida;
            output = new FileOutputStream(
                    new File(nombreDocumento));
            JasperExportManager.exportReportToPdfStream(jasperPrint, output);

        } catch (NamingException | SQLException | JRException | IOException e) {
            Logger.getLogger(ReportUtil.class.getName()).log(Level.SEVERE, e.getLocalizedMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace(System.out);
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ex) {
                    ex.printStackTrace(System.out);
                }
            }
        }

        return nombreReporteSalida;
    }

}
