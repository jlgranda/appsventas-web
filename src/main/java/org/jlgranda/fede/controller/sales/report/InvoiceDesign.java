/*
 * Copyright (C) 2019 jlgranda
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
package org.jlgranda.fede.controller.sales.report;

import java.math.BigDecimal;
import java.util.Map;
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.jasper.builder.export.JasperOdtExporterBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import static net.sf.dynamicreports.report.builder.DynamicReports.cmp;
import static net.sf.dynamicreports.report.builder.DynamicReports.col;
import static net.sf.dynamicreports.report.builder.DynamicReports.exp;
import static net.sf.dynamicreports.report.builder.DynamicReports.export;
import static net.sf.dynamicreports.report.builder.DynamicReports.report;
import static net.sf.dynamicreports.report.builder.DynamicReports.sbt;
import static net.sf.dynamicreports.report.builder.DynamicReports.stl;
import static net.sf.dynamicreports.report.builder.DynamicReports.type;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.ComponentBuilder;
import net.sf.dynamicreports.report.builder.component.HorizontalListBuilder;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRDataSource;
import org.jlgranda.fede.model.sales.Detail;
import org.jlgranda.fede.model.sales.Invoice;
import org.omnifaces.el.functions.Dates;

/**
 *
 * @author jlgranda
 */
public class InvoiceDesign {

    private Invoice invoice = new Invoice();
    private AggregationSubtotalBuilder<BigDecimal> totalSum;
    
    public InvoiceDesign(Invoice invoice, Map<String, String> settings) {
        this.invoice = invoice;
        build(this.invoice, settings);
    }
    
    
    private JasperReportBuilder build(Invoice invoice, Map<String, String> settings) {
        
        JasperReportBuilder report = report();
        
        // init styles
        StyleBuilder columnStyle = stl.style(Templates.columnStyle).setBorder(stl.pen1Point());
        StyleBuilder subtotalStyle = stl.style(columnStyle).bold();
        StyleBuilder shippingStyle = stl.style(Templates.boldStyle).setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT);

        // init columns
        TextColumnBuilder<Integer> rowNumberColumn = col.reportRowNumberColumn().setFixedColumns(2).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER);
        TextColumnBuilder<String> descriptionColumn = col.column("Description", "description", type.stringType()).setFixedWidth(250);
        TextColumnBuilder<Integer> quantityColumn = col.column("Quantity", "quantity", type.integerType()).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER);
        TextColumnBuilder<BigDecimal> unitPriceColumn = col.column("Unit Price", "unitprice", Templates.currencyType);
        TextColumnBuilder<String> taxColumn = col.column("Tax", exp.text("12%")).setFixedColumns(3);
        // price = unitPrice * quantity
        TextColumnBuilder<BigDecimal> priceColumn = unitPriceColumn.multiply(quantityColumn).setTitle("Price").setDataType(Templates.currencyType);
        // vat = price * tax
        TextColumnBuilder<BigDecimal> vatColumn = priceColumn.multiply(BigDecimal.valueOf(1)).setTitle("VAT").setDataType(Templates.currencyType);
        // total = price + vat
        TextColumnBuilder<BigDecimal> totalColumn = priceColumn.add(vatColumn).setTitle("Total Price").setDataType(Templates.currencyType).setRows(2).setStyle(subtotalStyle);
        // init subtotals
        totalSum = sbt.sum(totalColumn).setLabel("Total:").setLabelStyle(Templates.boldStyle);

        // configure report
        report.setTemplate(Templates.reportTemplate)
              .setColumnStyle(columnStyle)
              .setSubtotalStyle(subtotalStyle)
              // columns
              //.columns(rowNumberColumn, quantityColumn, descriptionColumn,  unitPriceColumn, totalColumn, priceColumn, taxColumn, vatColumn)
              .columns(rowNumberColumn, quantityColumn, descriptionColumn,  unitPriceColumn, totalColumn)
//              .columnGrid(rowNumberColumn, descriptionColumn, quantityColumn, unitPriceColumn, grid.horizontalColumnGridList().add(totalColumn).newRow().add(priceColumn, taxColumn, vatColumn))
              .columnGrid(rowNumberColumn, quantityColumn, descriptionColumn, unitPriceColumn, totalColumn)
              // subtotals
              .subtotalsAtSummary(totalSum, sbt.sum(priceColumn), sbt.sum(quantityColumn))
              // band components
              .title(Templates.createTitleComponent("Invoice No.: " + invoice.getCode()), cmp.horizontalList()
                                                                                                     .setStyle(stl.style(10))
                                                                                                     .setGap(50)
                                                                                                     .add(cmp.hListCell(createCustomerComponent("", invoice))
                                                                                                             .heightFixedOnTop()), cmp.verticalGap(10))
              .pageFooter(Templates.footerComponent)
              .summary(cmp.text(invoice.getTotal()).setValueFormatter(Templates.createCurrencyValueFormatter("Shipping:")).setStyle(shippingStyle),
                       cmp.horizontalList(cmp.text("Payment terms: 30 days").setStyle(Templates.bold12CenteredStyle), cmp.text(new TotalPaymentExpression()).setStyle(Templates.bold12CenteredStyle)),
                       cmp.verticalGap(30), cmp.text("Thank you for your business").setStyle(Templates.bold12CenteredStyle))
              .setDataSource(createDataSource(invoice));

        //Enviar a archivo
        try {

            //AdhocManager adhocManager = AdhocManager.getInstance(new AdhocToXmlTransform(), new XmlToAdhocTransform());
            //JasperReportBuilder reportBuilder = adhocManager.createReport(, new AdhocCustomizerReport.ReportCustomizer(invoice, settings));
            //report.setDataSource(createDataSource(invoice));
            //ODT JRTextExporter
            JasperOdtExporterBuilder odtExporter = export.odtExporter("/tmp/" + invoice.getSequencial() + ".odt");
            report.toOdt(odtExporter);
 
        } catch (DRException e) {
            e.printStackTrace();
        } /*catch (JRException ex) {
            Logger.getLogger(AdhocCustomizerReport.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        return report;
    }
    
    private ComponentBuilder<?, ?> createCustomerComponent(String label, Invoice invoice) {
        HorizontalListBuilder list = cmp.horizontalList().setBaseStyle(stl.style().setTopBorder(stl.pen1Point()).setLeftPadding(10));
        addCustomerAttribute(list, "Cliente: ", invoice.getOwner().getFullName());
        addCustomerAttribute(list, "C.I./RUC: ", invoice.getOwner().getCode());
        addCustomerAttribute(list, "Dirección: ", invoice.getOwner().getDescription());
        addCustomerAttribute(list, "Teléfono: ", invoice.getOwner().getMobileNumber());
        addCustomerAttribute(list, "Fecha: ", Dates.formatDate(invoice.getCreatedOn(), "d/MM/yyyy HH:mm"));
        addCustomerAttribute(list, "Mesa: ", invoice.getBoardNumber());
        return cmp.verticalList(cmp.text(label).setStyle(Templates.boldStyle), list);
    }

    private void addCustomerAttribute(HorizontalListBuilder list, String label, String value) {
        if (value != null) {
            list.add(cmp.text(label + ":").setFixedColumns(8).setStyle(Templates.boldStyle), cmp.text(value)).newRow();
        }
    }

    private class TotalPaymentExpression extends AbstractSimpleExpression<String> {
        private static final long serialVersionUID = 1L;

        @Override
        public String evaluate(ReportParameters reportParameters) {
            BigDecimal total = reportParameters.getValue(totalSum);
            BigDecimal shipping = total.add(invoice.getPaymentsCash());
            return "Total payment: " + Templates.currencyType.valueToString(shipping, reportParameters.getLocale());
        }
    }
    
    private JRDataSource createDataSource(Invoice invoice) {
        DRDataSource dataSource = new DRDataSource("cantidad", "descripcion", "preciounitario", "subtotal");
        for (Detail detail : invoice.getDetails()) {
            dataSource.add(detail.getAmount(), detail.getProduct().getName(), detail.getPrice(), detail.getPrice().multiply(detail.getAmount()));
        }
        //Agregar el descuento como item
        if (BigDecimal.ZERO.compareTo(invoice.getPaymentsDiscount()) < 0){
            BigDecimal discount = invoice.getPaymentsDiscount().multiply(BigDecimal.valueOf(-1));
            dataSource.add(1.0f, "Descuento", discount, discount);
        }
        return dataSource;
    }
}
