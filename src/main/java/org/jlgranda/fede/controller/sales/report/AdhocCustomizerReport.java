/**
 * DynamicReports - Free Java reporting library for creating reports dynamically
 *
 * Copyright (C) 2010 - 2016 Ricardo Mariaca
 * http://www.dynamicreports.org
 *
 * This file is part of DynamicReports.
 *
 * DynamicReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DynamicReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with DynamicReports. If not, see <http://www.gnu.org/licenses/>.
 */
package org.jlgranda.fede.controller.sales.report;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.PrinterName;
import static net.sf.dynamicreports.report.builder.DynamicReports.*;

import net.sf.dynamicreports.adhoc.AdhocManager;
import net.sf.dynamicreports.adhoc.configuration.AdhocCalculation;
import net.sf.dynamicreports.adhoc.configuration.AdhocColumn;
import net.sf.dynamicreports.adhoc.configuration.AdhocConfiguration;
import net.sf.dynamicreports.adhoc.configuration.AdhocReport;
import net.sf.dynamicreports.adhoc.configuration.AdhocSubtotal;
import net.sf.dynamicreports.adhoc.configuration.AdhocSubtotalPosition;
import net.sf.dynamicreports.adhoc.report.DefaultAdhocReportCustomizer;
import net.sf.dynamicreports.jasper.base.export.JasperHtmlExporter;
import net.sf.dynamicreports.jasper.base.export.JasperOdtExporter;
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.jasper.builder.export.JasperHtmlExporterBuilder;
import net.sf.dynamicreports.jasper.builder.export.JasperOdtExporterBuilder;
import net.sf.dynamicreports.jasper.builder.export.JasperPdfExporterBuilder;
import net.sf.dynamicreports.jasper.builder.export.JasperTextExporterBuilder;
import net.sf.dynamicreports.report.builder.ReportBuilder;
import net.sf.dynamicreports.report.constant.Orientation;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.definition.datatype.DRIDataType;
import net.sf.dynamicreports.report.definition.expression.DRIExpression;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.engine.export.JRTextExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimplePrintServiceExporterConfiguration;
import org.jlgranda.fede.model.sales.Detail;
import org.jlgranda.fede.model.sales.Invoice;
import org.jlgranda.fede.model.sales.Payment;

/**
 * @author Ricardo Mariaca (r.mariaca@dynamicreports.org)
 * @adaptedby José Luis Granda (jlgranda@gmail.com)
 */
public class AdhocCustomizerReport {
    
    public AdhocCustomizerReport(Invoice invoice, Map<String, String> settings) {
        build(invoice, settings);
    }

    private void build(Invoice invoice, Map<String, String> settings) {
        AdhocConfiguration configuration = new AdhocConfiguration();
        AdhocReport report = new AdhocReport();
        configuration.setReport(report);

        int pageWidth = Templates.reportTemplate.getReportTemplate().getPageWidth()- 40;
        //columns
        AdhocColumn column = new AdhocColumn();
        column.setName("cantidad");
        column.setWidth(calculePorcentaje(pageWidth, 12));
        report.addColumn(column);
        column = new AdhocColumn();
        column.setName("descripcion");
        column.setWidth(calculePorcentaje(pageWidth, 58));
        report.addColumn(column);
        column = new AdhocColumn();
        column.setName("preciounitario");
        column.setWidth(calculePorcentaje(pageWidth, 15));
        report.addColumn(column);
        column = new AdhocColumn();
        column.setName("subtotal");
        column.setWidth(calculePorcentaje(pageWidth,15));
        report.addColumn(column);

//		//groups
//		AdhocGroup group = new AdhocGroup();
//		group.setName("invoice");
//		report.addGroup(group);
        //subtotal
//		AdhocSubtotal subtotal = new AdhocSubtotal();
//		subtotal.setName("quantity");
//		subtotal.setCalculation(AdhocCalculation.COUNT);
//		report.addSubtotal(subtotal);
        AdhocSubtotal subtotal = new AdhocSubtotal();
        subtotal.setCalculation(AdhocCalculation.SUM);
        subtotal.setName("subtotal");
        subtotal.setPosition(AdhocSubtotalPosition.PAGE_FOOTER);
        report.addSubtotal(subtotal);
//		//sorts
//		AdhocSort sort = new AdhocSort();
//		sort.setName("item");
//		report.addSort(sort);

        try {

            JasperReportBuilder reportBuilder = AdhocManager.createReport(configuration.getReport(), new ReportCustomizer(invoice, settings));
            reportBuilder.setDataSource(createDataSource(invoice));
            
            
            
            //PDF
//            JasperPdfExporterBuilder pdfExporter = export.pdfExporter("/tmp/" + invoice.getSequencial() + ".pdf")
//                    .setEncrypted(false);
//            reportBuilder.toPdf(pdfExporter);
            
            //HTML 
//            JasperHtmlExporterBuilder htmlExporter = export.htmlExporter("/tmp/" + invoice.getSequencial() + ".html");
//            reportBuilder.toHtml(htmlExporter);
            
            //TXT JRTextExporter
//            JasperTextExporterBuilder txtExporter = export.textExporter("/tmp/" + invoice.getSequencial() + ".txt").setPageWidthInChars(52);
//            reportBuilder.toText(txtExporter);
            
            //ODT JRTextExporter
            JasperOdtExporterBuilder odtExporter = export.odtExporter("/tmp/" + invoice.getSequencial() + ".odt");
            reportBuilder.toOdt(odtExporter);
            
            //Directamente a la impresora
//            PrintRequestAttributeSet printRequestAttributeSet = new HashPrintRequestAttributeSet();
//            printRequestAttributeSet.add(MediaSizeName.ISO_A6);
//            printRequestAttributeSet.add(new JobName(invoice.getSequencial(), null));
    
//            PrintServiceAttributeSet printServiceAttributeSet = new HashPrintServiceAttributeSet();
//            //printServiceAttributeSet.add(new PrinterName("Epson Stylus 820 ESC/P 2", null));
//            //printServiceAttributeSet.add(new PrinterName("hp LaserJet 1320 PCL 6", null));
//            printServiceAttributeSet.add(new PrinterName("Cups-PDF", null));
            

//            JRPrintServiceExporter exporter = new JRPrintServiceExporter();
//            exporter.setExporterInput(new SimpleExporterInput(reportBuilder.toJasperPrint()));
//            SimplePrintServiceExporterConfiguration simplePrintServiceExporterConfiguration = new SimplePrintServiceExporterConfiguration();
//            simplePrintServiceExporterConfiguration.setPrintRequestAttributeSet(printRequestAttributeSet);
//            simplePrintServiceExporterConfiguration.setPrintServiceAttributeSet(printServiceAttributeSet);
//            simplePrintServiceExporterConfiguration.setDisplayPageDialog(false);
//            simplePrintServiceExporterConfiguration.setDisplayPrintDialog(false);
//            exporter.setConfiguration(simplePrintServiceExporterConfiguration);
//            exporter.exportReport();
        } catch (DRException e) {
            e.printStackTrace();
        } /*catch (JRException ex) {
            Logger.getLogger(AdhocCustomizerReport.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }

    private JRDataSource createDataSource(Invoice invoice) {
        DRDataSource dataSource = new DRDataSource("cantidad", "descripcion", "preciounitario", "subtotal");
        for (Detail detail : invoice.getDetails()) {
            dataSource.add(detail.getAmount(), detail.getProduct().getName(), detail.getPrice(), detail.getPrice().multiply(BigDecimal.valueOf(detail.getAmount())));
        }
        //Agregar el descuento como item
        if (BigDecimal.ZERO.compareTo(invoice.getPaymentsDiscount()) < 0){
            BigDecimal discount = invoice.getPaymentsDiscount().multiply(BigDecimal.valueOf(-1));
            dataSource.add(1.0f, "Descuento", discount, discount);
        }
        return dataSource;
    }

    public static void main(String[] args) {
        new AdhocCustomizerReport(new Invoice(), null);
    }

    public static Integer calculePorcentaje(int pageWidth, int porcentaje) {
        double factor = (porcentaje / (double) 100);
        int valor = (int) (pageWidth * factor);
        //System.out.println(">>> pageWidth: " + pageWidth + ", pocentaje:" + porcentaje + ", factor" + factor+ ", valor " + valor);
        return valor;
    }

    private class ReportCustomizer extends DefaultAdhocReportCustomizer {

        Invoice invoice;
        Map<String, String> settings = new HashMap<>();
        public ReportCustomizer(Invoice invoice, Map<String, String> settings) {
            this.invoice = invoice;
            this.settings = settings;
        }

        /**
         * If you want to add some fixed content to a report that is not needed
         * to store in the xml file. For example you can add default page
         * header, footer, default fonts,...
         */
        @Override
        public void customize(ReportBuilder<?> report, AdhocReport adhocReport) throws DRException {
            super.customize(report, adhocReport);
            //default report values
            report.setTemplate(Templates.reportTemplate);
            //No title, the document is pre printed
            //report.title(Templates.createTitleComponent(invoice.getOwner().getFullName()));
            //Header
            report.addPageHeader(Templates.createInvoiceHeaderComponent(this.invoice, this.settings));
            //a fixed page footer that user cannot change, this customization is not stored in the xml file
            report.pageFooter(Templates.jlgrandaComponent, Templates.footerComponent);
        }

        /**
         * This method returns a field type from a given field name.
         */
        @Override
        protected DRIDataType<?, ?> getFieldType(String name) {

            if (name.equals("cantidad")) {
                return type.floatType();
            }
            if (name.equals("descripcion")) {
                return type.stringType();
            }
            if (name.equals("preciounitario")) {
                return type.bigDecimalType();
            }
            if (name.equals("subtotal")) {
                return type.bigDecimalType();
            }
            return super.getFieldType(name);
        }

        /**
         * If a user doesn’t specify a column title, getColumnTitle is evaluated
         * and the return value is used as a column title.
         */
        @Override
        protected String getFieldLabel(String name) {
            if (name.equals("invoice")) {
                return "Factura";
            }
            if (name.equals("descripcion")) {
                return "Descripción";
            }
            if (name.equals("cantidad")) {
                return "Cant.";
            }
            if (name.equals("preciounitario")) {
                return "V. Unit.";
            }
            if (name.equals("subtotal")) {
                return "V. Total";
            }
            return name;
        }

    }
}
