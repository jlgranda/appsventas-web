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

import static net.sf.dynamicreports.report.builder.DynamicReports.*;

import java.awt.Color;
import java.util.Locale;
import java.util.Map;

import net.sf.dynamicreports.report.base.expression.AbstractValueFormatter;
import net.sf.dynamicreports.report.builder.ReportTemplateBuilder;
import net.sf.dynamicreports.report.builder.component.ComponentBuilder;
import net.sf.dynamicreports.report.builder.datatype.BigDecimalType;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.builder.tableofcontents.TableOfContentsCustomizerBuilder;
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.constant.VerticalTextAlignment;
import net.sf.dynamicreports.report.definition.ReportParameters;
import org.jlgranda.fede.model.sales.Invoice;
import org.jpapi.model.TaxType;
import org.omnifaces.el.functions.Dates;

/**
 * @author Ricardo Mariaca (r.mariaca@dynamicreports.org)
 */
public class Templates {

    /**
     * Constant <code>rootStyle</code>
     */
    public static final StyleBuilder rootStyle;
    /**
     * Constant <code>rootStyle</code>
     */
    public static final StyleBuilder footerStyle;
    /**
     * Constant <code>boldStyle</code>
     */
    public static final StyleBuilder boldStyle;
    /**
     * Constant <code>italicStyle</code>
     */
    public static final StyleBuilder italicStyle;
    /**
     * Constant <code>boldCenteredStyle</code>
     */
    public static final StyleBuilder boldCenteredStyle;
    /**
     * Constant <code>bold10CenteredStyle</code>
     */
    public static final StyleBuilder bold10CenteredStyle;
    /**
     * Constant <code>bold12CenteredStyle</code>
     */
    public static final StyleBuilder bold12CenteredStyle;
    /**
     * Constant <code>bold18CenteredStyle</code>
     */
    public static final StyleBuilder bold18CenteredStyle;
    /**
     * Constant <code>bold22CenteredStyle</code>
     */
    public static final StyleBuilder bold22CenteredStyle;
    /**
     * Constant <code>columnStyle</code>
     */
    public static final StyleBuilder columnStyle;
    /**
     * Constant <code>columnTitleStyle</code>
     */
    public static final StyleBuilder columnTitleStyle;
    /**
     * Constant <code>groupStyle</code>
     */
    public static final StyleBuilder groupStyle;
    /**
     * Constant <code>subtotalStyle</code>
     */
    public static final StyleBuilder subtotalStyle;

    /**
     * Constant <code>reportTemplate</code>
     */
    public static final ReportTemplateBuilder reportTemplate;
    /**
     * Constant <code>currencyType</code>
     */
    public static final CurrencyType currencyType;
    /**
     * Constant <code>dynamicReportsComponent</code>
     */
    //public static final ComponentBuilder<?, ?> dynamicReportsComponent = new ComponentBuilder<ComponentBuilder<T, U>, DRComponent>();
    /**
     * Constant <code>footerComponent</code>
     */
    public static final ComponentBuilder<?, ?> footerComponent;

    static {
        rootStyle = stl.style().setPadding(1);
        footerStyle = stl.style().setPadding(1).setFontSize(8);
        boldStyle = stl.style(rootStyle).bold();
        italicStyle = stl.style(rootStyle).italic();
        boldCenteredStyle = stl.style(boldStyle).setTextAlignment(HorizontalTextAlignment.CENTER, VerticalTextAlignment.MIDDLE);
        bold10CenteredStyle = stl.style(boldCenteredStyle).setFontSize(10);
        bold12CenteredStyle = stl.style(boldCenteredStyle).setFontSize(12);
        bold18CenteredStyle = stl.style(boldCenteredStyle).setFontSize(18);
        bold22CenteredStyle = stl.style(boldCenteredStyle).setFontSize(22);
        columnStyle = stl.style(rootStyle).setVerticalTextAlignment(VerticalTextAlignment.MIDDLE);
        columnTitleStyle = stl.style(columnStyle).setBorder(stl.penDotted()).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER).setBackgroundColor(Color.WHITE).bold();
        groupStyle = stl.style(boldStyle).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT).setFirstLineIndent(0);
        subtotalStyle = stl.style(boldStyle).setTopBorder(stl.penDotted());

        StyleBuilder crosstabGroupStyle = stl.style(columnTitleStyle);
        StyleBuilder crosstabGroupTotalStyle = stl.style(columnTitleStyle).setBackgroundColor(Color.WHITE);
        StyleBuilder crosstabGrandTotalStyle = stl.style(columnTitleStyle).setBackgroundColor(Color.WHITE);
        StyleBuilder crosstabCellStyle = stl.style(columnStyle).setBorder(stl.pen1Point());

        TableOfContentsCustomizerBuilder tableOfContentsCustomizer = tableOfContentsCustomizer().setHeadingStyle(0, stl.style(rootStyle).bold());

        reportTemplate = template().setPageFormat(PageType.HALF_A5_EMPORIO)
                .setLocale(Locale.ENGLISH)
                .setColumnStyle(columnStyle)
                .setColumnTitleStyle(columnTitleStyle)
                .setGroupStyle(groupStyle)
                .setGroupTitleStyle(groupStyle)
                .setSubtotalStyle(subtotalStyle)
                .setCrosstabGroupStyle(crosstabGroupStyle)
                .setCrosstabGroupTotalStyle(crosstabGroupTotalStyle)
                .setCrosstabGrandTotalStyle(crosstabGrandTotalStyle)
                .setCrosstabCellStyle(crosstabCellStyle)
                .setTableOfContentsCustomizer(tableOfContentsCustomizer);

        currencyType = new CurrencyType();

        //footerComponent = cmp.pageXofY().setStyle(stl.style(boldCenteredStyle).setTopBorder(stl.pen1Point()));
        //footerComponent = cmp.text("AppsVentas por jlgranda.com").setStyle(stl.style(boldCenteredStyle).setTopBorder(stl.pen1Point()));
        footerComponent = cmp.text("De Loja a domicilio www.cafesdeloja.com").setStyle(stl.style(footerStyle).setTopBorder(stl.pen1Point()));
    }

    /**
     * Creates custom component which is possible to add to any report band
     * component
     *
     * @param label a {@link java.lang.String} object.
     * @return a
     * {@link net.sf.dynamicreports.report.builder.component.ComponentBuilder}
     * object.
     */
    public static ComponentBuilder<?, ?> createTitleComponent(String label) {
        return cmp.horizontalList()
                .add(cmp.text(label).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT))
                .newRow()
                .add(cmp.line())
                .newRow()
                .add(cmp.verticalGap(6));
    }

    /**
     * <p>
     * createCurrencyValueFormatter.</p>
     *
     * @param label a {@link java.lang.String} object.
     * @return a
     * {@link net.sf.dynamicreports.examples.Templates.CurrencyValueFormatter}
     * object.
     */
    public static CurrencyValueFormatter createCurrencyValueFormatter(String label) {
        return new CurrencyValueFormatter(label);
    }

    private static String corregirCorreoElectronico(String email) {
        if (email.endsWith("@emporiolojano.com")) {
            return "-";
        }
        return email;
    }

    public static class CurrencyType extends BigDecimalType {

        private static final long serialVersionUID = 1L;

        @Override
        public String getPattern() {
            return "$ #,###.00";
        }
    }

    private static class CurrencyValueFormatter extends AbstractValueFormatter<String, Number> {

        private static final long serialVersionUID = 1L;

        private String label;

        public CurrencyValueFormatter(String label) {
            this.label = label;
        }

        @Override
        public String format(Number value, ReportParameters reportParameters) {
            return label + currencyType.valueToString(value, reportParameters.getLocale());
        }
    }

    /**
     * Creates custom component which is possible to add to any report band
     * component
     *
     * @param invoice
     * @param settings
     * @return
     */
    public static ComponentBuilder<?, ?> createInvoiceHeaderComponent(Invoice invoice, Map<String, String> settings) {
        if (settings.containsKey("app.fede.report.invoice.fontName")) {
            rootStyle.setFontName(settings.get("app.fede.report.invoice.fontName"));
        }
        if (settings.containsKey("app.fede.report.invoice.fontSize")) {
            rootStyle.setFontSize(Integer.valueOf(settings.get("app.fede.report.invoice.fontSize")));
        }
        if (settings.containsKey("app.fede.report.invoice.fontStyle") && "bold".equalsIgnoreCase(settings.get("app.fede.report.invoice.fontStyle"))) {
            rootStyle.bold();
        }

        return cmp.horizontalList()
                .add(cmp.verticalGap(Integer.valueOf(settings.get("app.fede.report.invoice.startLine"))))
                .newRow()
                .add(cmp.text("Cliente: " + invoice.getOwner().getFullName()).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT))
                .newRow()
                .add(cmp.text("C.I/RUC: " + invoice.getOwner().getCode()).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT))
                .newRow()
                .add(cmp.text("Dirección: " + invoice.getOwner().getDescription()).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT))
                .newRow()
                .add(cmp.text("Teléfono: " + invoice.getOwner().getMobileNumber()).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT))
                .newRow()
                .add(cmp.text("Correo: " + corregirCorreoElectronico(invoice.getOwner().getEmail())).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT))
                .newRow()
                .add(cmp.text("Fecha: " + Dates.formatDate(invoice.getCreatedOn(), "d/MM/yyyy HH:mm")).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT))
                .newRow()
                .add(cmp.text("Mesa: " + invoice.getBoardNumber() + " / Comanda: " + invoice.getCode() + " / Servicio: " + invoice.getAuthor().getFirstname()).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT))
                .add(cmp.verticalGap(5));
    }
    /**
     * Creates custom component which is possible to add to any report band
     * component
     *
     * @param invoice
     * @param settings
     * @return
     */
    public static ComponentBuilder<?, ?> createInvoiceSummary(Invoice invoice, Map<String, String> settings) {
        if (settings.containsKey("app.fede.report.invoice.fontName")) {
            rootStyle.setFontName(settings.get("app.fede.report.invoice.fontName"));
        }
        if (settings.containsKey("app.fede.report.invoice.fontSize")) {
            rootStyle.setFontSize(Integer.valueOf(settings.get("app.fede.report.invoice.fontSize")));
        }
        if (settings.containsKey("app.fede.report.invoice.fontStyle") && "bold".equalsIgnoreCase(settings.get("app.fede.report.invoice.fontStyle"))) {
            rootStyle.bold();
        }

//        report.summary(cmp.text(this.invoice.getTotalTax(TaxType.IVA)).setValueFormatter(Templates.createCurrencyValueFormatter("IVA 12%:")));
//            report.summary(cmp.text(this.invoice.getTotal().add(this.invoice.getTotalTax(TaxType.IVA))).setValueFormatter(Templates.createCurrencyValueFormatter("Total a pagar:")));
//            
        return cmp.horizontalList()
                .add(cmp.verticalGap(Integer.valueOf(settings.get("app.fede.report.invoice.startLine"))))
                .newRow()
                .add(cmp.text(invoice.getTotalTax(TaxType.IVA)).setValueFormatter(Templates.createCurrencyValueFormatter("IVA 12%: ")).setStyle(subtotalStyle).setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT))
                .newRow()
                .add(cmp.text(invoice.getTotalSinImpuesto().add(invoice.getTotalTax(TaxType.IVA))).setValueFormatter(Templates.createCurrencyValueFormatter("Total a pagar: ")).setStyle(subtotalStyle).setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT))
                .newRow();
    }
}
