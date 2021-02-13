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
import static net.sf.dynamicreports.report.builder.DynamicReports.*;

import net.sf.dynamicreports.adhoc.AdhocManager;
import net.sf.dynamicreports.adhoc.configuration.AdhocCalculation;
import net.sf.dynamicreports.adhoc.configuration.AdhocColumn;
import net.sf.dynamicreports.adhoc.configuration.AdhocConfiguration;
import net.sf.dynamicreports.adhoc.configuration.AdhocReport;
import net.sf.dynamicreports.adhoc.configuration.AdhocSubtotal;
import net.sf.dynamicreports.adhoc.configuration.AdhocSubtotalPosition;
import net.sf.dynamicreports.adhoc.report.DefaultAdhocReportCustomizer;
import net.sf.dynamicreports.adhoc.transformation.AdhocToXmlTransform;
import net.sf.dynamicreports.adhoc.transformation.XmlToAdhocTransform;
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.jasper.builder.export.JasperPdfExporterBuilder;
import net.sf.dynamicreports.report.builder.ReportBuilder;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.definition.datatype.DRIDataType;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRDataSource;
import org.jlgranda.fede.model.sales.Invoice;
import org.jlgranda.fede.model.sales.Detail;

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

        int pageWidth = Templates.reportTemplate.getReportTemplate().getPageWidth() - 20;
        
        //columns
        AdhocColumn column = new AdhocColumn();
        column.setName("cantidad");
        column.setTitle("Cant.");
        column.setWidth(calculePorcentaje(pageWidth, 14));
        report.addColumn(column);
        
        column = new AdhocColumn();
        column.setName("descripcion");
        column.setTitle("Descripción");
        column.setWidth(calculePorcentaje(pageWidth, 56));
        report.addColumn(column);
        
        column = new AdhocColumn();
        column.setName("preciounitario");
        column.setTitle("P.U.");
        column.setWidth(calculePorcentaje(pageWidth, 14));
        report.addColumn(column);
        
        column = new AdhocColumn();
        column.setName("subtotal");
        column.setTitle("Subtotal");
        column.setWidth(calculePorcentaje(pageWidth, 16));
        report.addColumn(column);
        
	//groups
        //AdhocGroup group = new AdhocGroup();
        //group.setName("invoice");
        //group.setHeaderLayout(AdhocGroupHeaderLayout.EMPTY);
        //report.addGroup(group);
        //subtotal
//		AdhocSubtotal subtotal = new AdhocSubtotal();
//		subtotal.setName("quantity");
//		subtotal.setCalculation(AdhocCalculation.COUNT);
//		report.addSubtotal(subtotal);
        AdhocSubtotal subtotal = new AdhocSubtotal();
        subtotal.setCalculation(AdhocCalculation.SUM);
        subtotal.setName("subtotal");

        //subtotal.setGroupName("invoice");
        subtotal.setPosition(AdhocSubtotalPosition.SUMMARY);
        report.addSubtotal(subtotal);        
        
//		//sorts
//		AdhocSort sort = new AdhocSort();
//		sort.setName("item");
//		report.addSort(sort);

        try {

            AdhocManager adhocManager = AdhocManager.getInstance(new AdhocToXmlTransform(), new XmlToAdhocTransform());
            JasperReportBuilder reportBuilder = adhocManager.createReport(configuration.getReport(), new ReportCustomizer(invoice, settings));
            reportBuilder.setDataSource(createDataSource(invoice));
            //reportBuilder.summary(cmp.text(invoice.getTotalTax(TaxType.IVA)).setValueFormatter(Templates.createCurrencyValueFormatter("IVA 12%:")));
            
            
            //PDF
            JasperPdfExporterBuilder pdfExporter = export.pdfExporter("/tmp/" + invoice.getSequencial() + ".pdf")
                    .setEncrypted(false);
            reportBuilder.toPdf(pdfExporter);
            
            //HTML 
//            JasperHtmlExporterBuilder htmlExporter = export.htmlExporter("/tmp/" + invoice.getSequencial() + ".html");
//            reportBuilder.toHtml(htmlExporter);
            
            //TXT JRTextExporter
//            JasperTextExporterBuilder txtExporter = export.textExporter("/tmp/" + invoice.getSequencial() + ".txt").setPageWidthInChars(52);
//            reportBuilder.toText(txtExporter);
            
            //ODT JRTextExporter
//            JasperOdtExporterBuilder odtExporter = export.odtExporter("/tmp/" + invoice.getSequencial() + ".odt");
//            reportBuilder.toOdt(odtExporter);
            
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
        if ( !Boolean.TRUE.equals(invoice.getPrintAlias()) ){
            for (Detail detail : invoice.getDetails()){
                if (detail.getAmount() != 0.0f){
                    BigDecimal subtotal = detail.getPrice().multiply(BigDecimal.valueOf(detail.getAmount()));
                    dataSource.add(detail.getAmount(), detail.getProduct().getName(), detail.getPrice(), subtotal);
                }
            }
            //Agregar el descuento como item
            if (BigDecimal.ZERO.compareTo(invoice.getPaymentsDiscount()) < 0){
                BigDecimal discount = invoice.getPaymentsDiscount().multiply(BigDecimal.valueOf(-1));
                dataSource.add(1.0f, "Descuento", discount, discount);
            }
        } else {
            dataSource.add(1.0f, invoice.getPrintAliasSummary(), invoice.getTotalSinImpuesto(), invoice.getTotalSinImpuesto());
        }
        return dataSource;
    }

    public static void main(String[] args) {
        AdhocCustomizerReport adhocCustomizerReport = new AdhocCustomizerReport(new Invoice(), null);
    }

    public static Integer calculePorcentaje(int pageWidth, int porcentaje) {
        double factor = (porcentaje / (double) 100);
        int valor = (int) (pageWidth * factor);
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
            
            report.summary(Templates.createInvoiceSummary(this.invoice, this.settings));
            //a fixed page footer that user cannot change, this customization is not stored in the xml file
            report.pageFooter(Templates.footerComponent);
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
                return "Factura No";
            }
            if (name.equals("descripcion")) {
                return "Descripción";
            }
            if (name.equals("cantidad")) {
                return "Cant.";
            }
            if (name.equals("preciounitario")) {
                return "P.U.";
            }
            if (name.equals("subtotal")) {
                return "Subtotal";
            }
            return name;
        }

    }
}
