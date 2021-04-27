/*
 * Copyright (C) 2021 kellypaulinc
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
package org.jlgranda.fede.controller;

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.AccountService;
import com.jlgranda.fede.ejb.CashBoxDetailService;
import com.jlgranda.fede.ejb.CashBoxGeneralService;
import com.jlgranda.fede.ejb.CashBoxService;
import com.jlgranda.fede.ejb.GeneralJournalService;
import com.jlgranda.fede.ejb.RecordDetailService;
import com.jlgranda.fede.ejb.RecordService;
import com.jlgranda.fede.ejb.sales.InvoiceService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.model.accounting.CashBox;
import org.jlgranda.fede.model.accounting.CashBoxDetail;
import org.jlgranda.fede.model.accounting.CashBoxGeneral;
import org.jlgranda.fede.model.accounting.GeneralJournal;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.accounting.RecordDetail;
import org.jlgranda.fede.model.accounting.RecordDetail.RecordTDetailType;
import org.jlgranda.fede.model.document.DocumentType;
import org.jlgranda.fede.model.document.EmissionType;
import org.jpapi.model.Group;
import org.jpapi.model.StatusType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kellypaulinc
 */
@Named
@ViewScoped
public class CashBoxHome extends FedeController implements Serializable {

    private static final long serialVersionUID = -1007161141552849702L;

    Logger logger = LoggerFactory.getLogger(CashBoxHome.class);

    @Inject
    private Subject subject;

    @Inject
    private OrganizationData organizationData;

    @Inject
    private SettingHome settingHome;

    @EJB
    private InvoiceService invoiceService;

    @EJB
    private AccountService accountService;

    @EJB
    private GeneralJournalService journalService;

    @EJB
    private RecordService recordService;

    @EJB
    private RecordDetailService recordDetailService;

    @EJB
    private CashBoxGeneralService cashBoxGeneralService;

    @EJB
    private CashBoxService cashBoxService;

    @EJB
    private CashBoxDetailService cashBoxDetailService;

    /**
     * Instancia de entidad <tt>CashBoxGeneral</tt> para edición manual
     */
    private CashBoxGeneral cashBoxGeneral;
    /**
     * Instancia de entidad <tt>CashBox</tt> para edición manual
     */
    private CashBox cashBox;
    /**
     * Instancia de entidad <tt>CashBoxDetail</tt> para edición manual
     */
    private CashBoxDetail cashBoxDetail;

    //Calcular resumen
    private BigDecimal grossSalesTotal;
    private BigDecimal discountTotal;
    private BigDecimal salesTotal;
    private BigDecimal purchasesTotal;
    private BigDecimal costTotal;
    private BigDecimal profilTotal;
    private Long paxTotal;
    private List<Object[]> listDiscount;

    //Calcular el monto y panel de depósito
    private boolean activePanelDeposit;
    private boolean activeSelectDeposit;
    private BigDecimal amountDeposit;
    private boolean activeButtonSelectDeposit;
    private Account selectedAccount;
    private Account depositAccount;

    //Calcular el resumen del cierre de caja
    private BigDecimal salesCash;
    private BigDecimal salesDedit;
    private BigDecimal salesCredit;
    private BigDecimal purchasesCash;
    private BigDecimal purchasesCredit;
    private BigDecimal transactionDebit;
    private BigDecimal transactionCredit;
    private BigDecimal transactionTotal;

    //Calcular el dinero obtenido del cierre de caja
    private BigDecimal cashTotal;
    private BigDecimal saldoCash;
    private boolean activeSelectMenuBill;
    private boolean activeSelectMenuMoney;

    //Desglosar el efectivo de Caja
    private List<CashBoxDetail> cashBoxBills;
    private List<CashBoxDetail> cashBoxMoneys;
    private BigDecimal subtotalBills;
    private BigDecimal subtotalMoneys;
    private boolean activePanelBreakdown;
    private BigDecimal totalBreakdown;
    private BigDecimal missingBreakdown;
    private BigDecimal excessBreakdown;

    //Obtener lista de CashBoxClosed
    private List<CashBox> cashBoxsClosed;
    private List<CashBoxDetail> cashBoxsClosedBills;
    private BigDecimal cashBoxsClosedBillsSubtotal;
    private List<CashBoxDetail> cashBoxsClosedMoneys;
    private BigDecimal cashBoxsClosedMoneysSubtotal;
    private CashBox cashBoxOpen;
    private boolean activeButtonCloseCash;

    @PostConstruct
    private void init() {
        int range = 0;
        try {
            range = Integer.valueOf(settingHome.getValue(SettingNames.DASHBOARD__SUMMARY_RANGE, "7"));
        } catch (java.lang.NumberFormatException nfe) {
            nfe.printStackTrace();
            range = 7;
        }
        //Inicialización de variables, objetos, métodos.
        setEnd(Dates.maximumDate(Dates.now()));
//        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * range)));
        setStart(Dates.minimumDate(getEnd()));
        setOutcome("close-cash");

        setCashBoxGeneral(cashBoxGeneralService.createInstance());
        setCashBox(cashBoxService.createInstance());
        setCashBoxDetail(cashBoxDetailService.createInstance());

        setGrossSalesTotal(BigDecimal.ZERO);
        setDiscountTotal(BigDecimal.ZERO);
        setSalesTotal(BigDecimal.ZERO);
        setPurchasesTotal(BigDecimal.ZERO);
        setCostTotal(BigDecimal.ZERO);
        setProfilTotal(BigDecimal.ZERO);
        setPaxTotal(0L);

//        setActiveSelectDeposit(false); //rendered
        setAmountDeposit(BigDecimal.ZERO);
        setActiveButtonSelectDeposit(true); //disabled
        setSelectedAccount(accountService.findUniqueByNamedQuery("Account.findByNameAndOrg", "CAJA", this.organizationData.getOrganization()));

        setSalesCash(BigDecimal.ZERO);
        setSalesDedit(BigDecimal.ZERO);
        setSalesCredit(BigDecimal.ZERO);
        setPurchasesCash(BigDecimal.ZERO);
        setPurchasesCredit(BigDecimal.ZERO);
        setTransactionDebit(BigDecimal.ZERO);
        setTransactionCredit(BigDecimal.ZERO);
        setTransactionTotal(BigDecimal.ZERO);

        setCashTotal(BigDecimal.ZERO);
        setSaldoCash(BigDecimal.ZERO);
//        setActiveSelectMenuBill(true);
//        setActiveSelectMenuMoney(true);

        setSubtotalBills(BigDecimal.ZERO);
        setSubtotalMoneys(BigDecimal.ZERO);
//        setActivePanelBreakdown(false); //rendered
        setTotalBreakdown(BigDecimal.ZERO);
        setMissingBreakdown(BigDecimal.ZERO);
        setExcessBreakdown(BigDecimal.ZERO);

//        setActiveButtonCloseCash(true); //disabled
        calculeSummaryToday();
        findCashBoxs();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //GETTER AND SETTER
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public CashBoxGeneral getCashBoxGeneral() {
        return cashBoxGeneral;
    }

    public void setCashBoxGeneral(CashBoxGeneral cashBoxGeneral) {
        this.cashBoxGeneral = cashBoxGeneral;
    }

    public CashBox getCashBox() {
        return cashBox;
    }

    public void setCashBox(CashBox cashBox) {
        this.cashBox = cashBox;
    }

    public CashBoxDetail getCashBoxDetail() {
        return cashBoxDetail;
    }

    public void setCashBoxDetail(CashBoxDetail cashBoxDetail) {
        this.cashBoxDetail = cashBoxDetail;
    }

    public BigDecimal getGrossSalesTotal() {
        return grossSalesTotal;
    }

    public void setGrossSalesTotal(BigDecimal grossSalesTotal) {
        this.grossSalesTotal = grossSalesTotal;
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    public void setDiscountTotal(BigDecimal discountTotal) {
        this.discountTotal = discountTotal;
    }

    public BigDecimal getSalesTotal() {
        return salesTotal;
    }

    public void setSalesTotal(BigDecimal salesTotal) {
        this.salesTotal = salesTotal;
    }

    public BigDecimal getPurchasesTotal() {
        return purchasesTotal;
    }

    public void setPurchasesTotal(BigDecimal purchasesTotal) {
        this.purchasesTotal = purchasesTotal;
    }

    public BigDecimal getCostTotal() {
        return costTotal;
    }

    public void setCostTotal(BigDecimal costTotal) {
        this.costTotal = costTotal;
    }

    public BigDecimal getProfilTotal() {
        return profilTotal;
    }

    public void setProfilTotal(BigDecimal profilTotal) {
        this.profilTotal = profilTotal;
    }

    public Long getPaxTotal() {
        return paxTotal;
    }

    public void setPaxTotal(Long paxTotal) {
        this.paxTotal = paxTotal;
    }

    public List<Object[]> getListDiscount() {
        return listDiscount;
    }

    public void setListDiscount(List<Object[]> listDiscount) {
        this.listDiscount = listDiscount;
    }

    public boolean isActivePanelDeposit() {
        return activePanelDeposit;
    }

    public void setActivePanelDeposit(boolean activePanelDeposit) {
        this.activePanelDeposit = activePanelDeposit;
    }

    public boolean isActiveSelectDeposit() {
        return activeSelectDeposit;
    }

    public void setActiveSelectDeposit(boolean activeSelectDeposit) {
        this.activeSelectDeposit = activeSelectDeposit;
    }

    public BigDecimal getAmountDeposit() {
        return amountDeposit;
    }

    public void setAmountDeposit(BigDecimal amountDeposit) {
        this.amountDeposit = amountDeposit;
    }

    public boolean isActiveButtonSelectDeposit() {
        return activeButtonSelectDeposit;
    }

    public void setActiveButtonSelectDeposit(boolean activeButtonSelectDeposit) {
        this.activeButtonSelectDeposit = activeButtonSelectDeposit;
    }

    public Account getSelectedAccount() {
        return selectedAccount;
    }

    public void setSelectedAccount(Account selectedAccount) {
        this.selectedAccount = selectedAccount;
    }

    public Account getDepositAccount() {
        return depositAccount;
    }

    public void setDepositAccount(Account depositAccount) {
        this.depositAccount = depositAccount;
    }

    public BigDecimal getSalesCash() {
        return salesCash;
    }

    public void setSalesCash(BigDecimal salesCash) {
        this.salesCash = salesCash;
    }

    public BigDecimal getSalesDedit() {
        return salesDedit;
    }

    public void setSalesDedit(BigDecimal salesDedit) {
        this.salesDedit = salesDedit;
    }

    public BigDecimal getSalesCredit() {
        return salesCredit;
    }

    public void setSalesCredit(BigDecimal salesCredit) {
        this.salesCredit = salesCredit;
    }

    public BigDecimal getPurchasesCash() {
        return purchasesCash;
    }

    public void setPurchasesCash(BigDecimal purchasesCash) {
        this.purchasesCash = purchasesCash;
    }

    public BigDecimal getPurchasesCredit() {
        return purchasesCredit;
    }

    public void setPurchasesCredit(BigDecimal purchasesCredit) {
        this.purchasesCredit = purchasesCredit;
    }

    public BigDecimal getTransactionDebit() {
        return transactionDebit;
    }

    public void setTransactionDebit(BigDecimal transactionDebit) {
        this.transactionDebit = transactionDebit;
    }

    public BigDecimal getTransactionCredit() {
        return transactionCredit;
    }

    public void setTransactionCredit(BigDecimal transactionCredit) {
        this.transactionCredit = transactionCredit;
    }

    public BigDecimal getTransactionTotal() {
        return transactionTotal;
    }

    public void setTransactionTotal(BigDecimal transactionTotal) {
        this.transactionTotal = transactionTotal;
    }

    public BigDecimal getCashTotal() {
        return cashTotal;
    }

    public void setCashTotal(BigDecimal cashTotal) {
        this.cashTotal = cashTotal;
    }

    public BigDecimal getSaldoCash() {
        return saldoCash;
    }

    public void setSaldoCash(BigDecimal saldoCash) {
        this.saldoCash = saldoCash;
    }

    public boolean isActiveSelectMenuBill() {
        return activeSelectMenuBill;
    }

    public void setActiveSelectMenuBill(boolean activeSelectMenuBill) {
        this.activeSelectMenuBill = activeSelectMenuBill;
    }

    public boolean isActiveSelectMenuMoney() {
        return activeSelectMenuMoney;
    }

    public void setActiveSelectMenuMoney(boolean activeSelectMenuMoney) {
        this.activeSelectMenuMoney = activeSelectMenuMoney;
    }

    public void activeSelectBill() {
        this.activeSelectMenuBill = true;
    }

    public void activeSelectMoney() {
        this.activeSelectMenuMoney = true;
    }

    public List<CashBoxDetail> getCashBoxBills() {
        return cashBoxBills;
    }

    public void setCashBoxBills(List<CashBoxDetail> cashBoxBills) {
        this.cashBoxBills = cashBoxBills;
    }

    public List<CashBoxDetail> getCashBoxMoneys() {
        return cashBoxMoneys;
    }

    public void setCashBoxMoneys(List<CashBoxDetail> cashBoxMoneys) {
        this.cashBoxMoneys = cashBoxMoneys;
    }

    public BigDecimal getSubtotalBills() {
        return subtotalBills;
    }

    public void setSubtotalBills(BigDecimal subtotalBills) {
        this.subtotalBills = subtotalBills;
    }

    public BigDecimal getSubtotalMoneys() {
        return subtotalMoneys;
    }

    public void setSubtotalMoneys(BigDecimal subtotalMoneys) {
        this.subtotalMoneys = subtotalMoneys;
    }

    public boolean isActivePanelBreakdown() {
        return activePanelBreakdown;
    }

    public void setActivePanelBreakdown(boolean activePanelBreakdown) {
        this.activePanelBreakdown = activePanelBreakdown;
    }

    public BigDecimal getTotalBreakdown() {
        return totalBreakdown;
    }

    public void setTotalBreakdown(BigDecimal totalBreakdown) {
        this.totalBreakdown = totalBreakdown;
    }

    public BigDecimal getMissingBreakdown() {
        return missingBreakdown;
    }

    public void setMissingBreakdown(BigDecimal missingBreakdown) {
        this.missingBreakdown = missingBreakdown;
    }

    public BigDecimal getExcessBreakdown() {
        return excessBreakdown;
    }

    public void setExcessBreakdown(BigDecimal surplusBreakdown) {
        this.excessBreakdown = surplusBreakdown;
    }

    public List<CashBox> getCashBoxsClosed() {
        if (cashBoxGeneral.getId() != null) {
            return cashBoxService.findByNamedQuery("CashBox.findByCashBoxGeneralAndStatus", this.cashBoxGeneral, CashBox.Status.CLOSED);
        } else {
            return cashBoxsClosed;
        }
    }

    public void setCashBoxsClosed(List<CashBox> cashBoxsClosed) {
        this.cashBoxsClosed = cashBoxsClosed;
    }

    public List<CashBoxDetail> cashBoxsClosedBills(CashBox cashBox) {
        this.cashBoxsClosedBills = new ArrayList<>();
        this.cashBoxsClosedBillsSubtotal = BigDecimal.ZERO;
        for (CashBoxDetail cashBoxDetail1 : cashBox.getCashBoxDetails()) {
            if (cashBoxDetail1.getDenomination_type().equals(CashBoxDetail.DenominationType.BILL)) {
                this.cashBoxsClosedBills.add(cashBoxDetail1);
                this.cashBoxsClosedBillsSubtotal = this.cashBoxsClosedBillsSubtotal.add(cashBoxDetail1.getAmount());
            }
        }
        return cashBoxsClosedBills;
    }

    public List<CashBoxDetail> cashBoxsClosedMoneys(CashBox cashBox) {
        this.cashBoxsClosedMoneys = new ArrayList<>();
        this.cashBoxsClosedMoneysSubtotal = BigDecimal.ZERO;
        for (CashBoxDetail cashBoxDetail2 : cashBox.getCashBoxDetails()) {
            if (cashBoxDetail2.getDenomination_type().equals(CashBoxDetail.DenominationType.MONEY)) {
                this.cashBoxsClosedMoneys.add(cashBoxDetail2);
                this.cashBoxsClosedMoneysSubtotal = this.cashBoxsClosedMoneysSubtotal.add(cashBoxDetail2.getAmount());
            }
        }
        return cashBoxsClosedMoneys;
    }

    public BigDecimal getCashBoxsClosedBillsSubtotal() {
        return cashBoxsClosedBillsSubtotal;
    }

    public BigDecimal getCashBoxsClosedMoneysSubtotal() {
        return cashBoxsClosedMoneysSubtotal;
    }

    public CashBox getCashBoxOpen() {
        if (this.cashBoxGeneral.getId() != null) {
            return cashBoxOpen = cashBoxService.findUniqueByNamedQuery("CashBox.findByCashBoxGeneralAndStatus", this.cashBoxGeneral, CashBox.Status.OPEN);
        } else {
            return cashBoxOpen;
        }
    }

    public void setCashBoxOpen(CashBox cashBoxOpen) {
        this.cashBoxOpen = cashBoxOpen;
    }

    public boolean isActiveButtonCloseCash() {
        if (this.cashBoxGeneral.getId() == null || this.cashBoxGeneral.getStatusCashBoxGeneral().equals(CashBoxGeneral.Status.CLOSED)) {
            return activeButtonCloseCash = true;
        } else {
            return activeButtonCloseCash = false;
        }
    }

    public void setActiveButtonCloseCash(boolean activeButtonCloseCash) {
        this.activeButtonCloseCash = activeButtonCloseCash;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MÉTODOS/FUNCIONES
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //VENTAS/COMPRAS: SUMMARY
    public void calculeSummaryToday() {
        calculeSummary(getStart(), getEnd());
        setListDiscount(getListDiscount(getStart(), getEnd()));
        calculeSummaryCash(getStart(), getEnd());
    }

    private void calculeSummary(Date _start, Date _end) {
        this.costTotal = BigDecimal.ZERO;
        List<Object[]> objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesDiscountBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end);
        objects.stream().forEach((Object[] object) -> {
            this.grossSalesTotal = (BigDecimal) object[0];
            this.discountTotal = (BigDecimal) object[1];
            this.salesTotal = (BigDecimal) object[2];
        });
        if (this.grossSalesTotal == null) {
            this.grossSalesTotal = BigDecimal.ZERO;
        }
        if (this.discountTotal == null) {
            this.discountTotal = BigDecimal.ZERO;
        }
        if (this.salesTotal == null) {
            this.salesTotal = BigDecimal.ZERO;
        }
        objects = invoiceService.findObjectsByNamedQueryWithLimit("FacturaElectronica.findTotalByEmissionTypeBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), _start, _end, EmissionType.PURCHASE_CASH);
        objects.stream().forEach((Object object) -> {
            this.purchasesTotal = (BigDecimal) object;
        });
        if (this.purchasesTotal == null) {
            this.purchasesTotal = BigDecimal.ZERO;
        }
        objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesPaxBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end);
        objects.stream().forEach((Object object) -> {
            this.paxTotal = (Long) object;
        });
        if (this.paxTotal == null) {
            this.paxTotal = 0L;
        }
        this.profilTotal = this.salesTotal.subtract(this.purchasesTotal.add(this.costTotal));
    }

    public List<Object[]> getListDiscount(Date _start, Date _end) {
        List<Object[]> objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceBussinesSalesDiscountBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end, BigDecimal.ZERO);
        return objects;
    }

    public BigDecimal getListDiscountTotal() {
        BigDecimal total = new BigDecimal(0);
        for (int i = 0; i < getListDiscount().size(); i++) {
            total = total.add((BigDecimal) getListDiscount().get(i)[4]);
        }
        return total;
    }

    //AJUSTE DE CIERRE DE CAJA
    private void calculeSummaryCash(Date _start, Date _end) {
        List<Object[]> objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesMethodBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end, "EFECTIVO");
        objects.stream().forEach((Object object) -> {
            this.salesCash = (BigDecimal) object;
        });
        if (this.salesCash == null) {
            this.salesCash = BigDecimal.ZERO;
        }
        objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesMethodBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end, "TARJETA DEBITO");
        objects.stream().forEach((Object object) -> {
            this.salesDedit = (BigDecimal) object;
        });
        if (this.salesDedit == null) {
            this.salesDedit = BigDecimal.ZERO;
        }
        objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesMethodBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end, "TARJETA CREDITO");
        objects.stream().forEach((Object object) -> {
            this.salesCredit = (BigDecimal) object;
        });
        if (this.salesCredit == null) {
            this.salesCredit = BigDecimal.ZERO;
        }
        objects = invoiceService.findObjectsByNamedQueryWithLimit("FacturaElectronica.findTotalByEmissionTypeBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), _start, _end, EmissionType.PURCHASE_CASH);
        objects.stream().forEach((Object object) -> {
            this.purchasesCash = (BigDecimal) object;
        });
        if (this.purchasesCash == null) {
            this.purchasesCash = BigDecimal.ZERO;
        }
        objects = invoiceService.findObjectsByNamedQueryWithLimit("FacturaElectronica.findTotalByEmissionTypePayBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), _start, _end, EmissionType.PURCHASE_CREDIT);
        objects.stream().forEach((Object object) -> {
            this.purchasesCredit = (BigDecimal) object;
        });
        if (this.purchasesCredit == null) {
            this.purchasesCredit = BigDecimal.ZERO;
        }
        objects = recordDetailService.findObjectsByNamedQueryWithLimit("RecordDetail.findTotalByAccountAndType", Integer.MAX_VALUE, this.selectedAccount, RecordTDetailType.DEBE, _start, _end, this.organizationData.getOrganization());
        objects.stream().forEach((Object object) -> {
            this.transactionDebit = (BigDecimal) object;
        });
        if (this.transactionDebit == null) {
            this.transactionDebit = BigDecimal.ZERO;
        }
        objects = recordDetailService.findObjectsByNamedQueryWithLimit("RecordDetail.findTotalByAccountAndType", Integer.MAX_VALUE, this.selectedAccount, RecordTDetailType.HABER, _start, _end, this.organizationData.getOrganization());
        objects.stream().forEach((Object object) -> {
            this.transactionCredit = (BigDecimal) object;
        });
        if (this.transactionCredit == null) {
            this.transactionCredit = BigDecimal.ZERO;
        }

        this.salesTotal = this.salesCash.add(this.salesDedit).add(this.salesCredit); //Suma de ventas en caja
        this.purchasesTotal = this.purchasesCash.add(this.purchasesCredit);//Suma de compras en caja
        this.transactionTotal = this.transactionDebit.subtract(this.transactionCredit);//Suma de transacciones en caja
        this.cashTotal = this.salesTotal.subtract(this.purchasesTotal).add(this.transactionTotal); //Valor de Caja según los Libros: Total Ventas - Total Compras y la + o - de Transacciones
        this.saldoCash = this.cashTotal.subtract(this.salesDedit.add(this.salesCredit)); //Dinero en efectivo de Caja
//        this.saldoCash = BigDecimal.ZERO;
    }

    public void findCashBoxs() { //Buscar CashBoxGeneral y CashBox del Cajero en caso de existencia
        CashBoxGeneral objectGeneral = cashBoxGeneralService.findUniqueByNamedQuery("CashBoxGeneral.findByCreatedOnAndOrg", getStart(), getEnd(), this.organizationData.getOrganization());
        if (objectGeneral != null) {
            this.cashBoxGeneral = objectGeneral;
        }
        if (this.cashBoxGeneral.getId() != null) {
            CashBox objectCash = cashBoxService.findUniqueByNamedQuery("CashBox.findByCashBoxGeneralAndOwner", this.cashBoxGeneral, this.subject);
            if (objectCash != null) {
                this.cashBox = objectCash;
            }
        }
        if (this.cashBox.getId() != null) {
            this.activePanelDeposit = false; //Ocultar Panel de Depósito si ya se existe el CashBox, y sólo se va a editar
            this.activePanelBreakdown = true; //Mostrar el Panel de Detalle ya existente
            reloadDataCashBox();//Recargar la vista con los datos del CashBox Instanciado
        } else {
            this.activePanelDeposit = this.saldoCash.compareTo(BigDecimal.ZERO) == 1; //Mostra u Ocultar el Panel de Depósito según el saldoCash > 0
        }
        this.activeSelectMenuBill = false; //Habilitar los Selects del Panel de Detalle para Billetes y Monedas
        this.activeSelectMenuMoney = false;
        
        getCashBoxOpen(); //Buscar un Cashox abierto
        if (cashBoxOpen == null) { //Deshabilitar/Habilitar el Panel de Depósito según la existencia del CashBoxOpen
            this.activePanelDeposit = true;
        } else this.activePanelDeposit = cashBoxOpen.getId() == null;
    }

    private void reloadDataCashBox() { //Recargar la vista con los datos del CashBox Instanciado
        this.cashBoxBills = new ArrayList();
        this.cashBoxMoneys = new ArrayList();
        this.subtotalBills = BigDecimal.ZERO;
        this.subtotalMoneys = BigDecimal.ZERO;
        this.totalBreakdown = BigDecimal.ZERO;
        for (CashBoxDetail cashboxDetail : this.cashBox.getCashBoxDetails()) {
            if (cashboxDetail.getDenomination_type().equals(CashBoxDetail.DenominationType.BILL)) {
                this.cashBoxBills.add(cashboxDetail);
                this.subtotalBills = this.subtotalBills.add(cashboxDetail.getAmount());
            } else if (cashboxDetail.getDenomination_type().equals(CashBoxDetail.DenominationType.MONEY)) {
                this.cashBoxMoneys.add(cashboxDetail);
                this.subtotalMoneys = this.subtotalMoneys.add(cashboxDetail.getAmount());
            }
            this.totalBreakdown = this.totalBreakdown.add(cashboxDetail.getAmount());
        };
        switch (this.totalBreakdown.compareTo(this.saldoCash)) {
            case 0:
                this.missingBreakdown = BigDecimal.ZERO;
                this.excessBreakdown = BigDecimal.ZERO;
                break;
            case -1:
                this.missingBreakdown = this.totalBreakdown.subtract(this.saldoCash);
                this.excessBreakdown = BigDecimal.ZERO;
                break;
            case 1:
                this.missingBreakdown = BigDecimal.ZERO;
                this.excessBreakdown = this.totalBreakdown.subtract(this.saldoCash);
                break;
            default:
                break;
        }
    }

    public void addCashDetail() { //Agrega un CashBoxDetail al CashBox Instanciado
        calculateValuerAndType(); //Calcular el valor y tipo según la denominación
        this.cashBoxDetail.setAmount(this.cashBoxDetail.getValuer().multiply(BigDecimal.valueOf(this.cashBoxDetail.getQuantity()))); //Calcular monto de denominación
        this.cashBox.addCashBoxDetail(this.cashBoxDetail);//Agregar el CashBoxDetail al CashBox Instanciado
        reloadDataCashBox(); //Recargar la vista con los datos del CashBox Instanciado
        this.cashBoxDetail = cashBoxDetailService.createInstance();//Preparar para un nuevo detalle del CashBox Instanciado
        cleanPanelDetail(); //Resetear los inserts del Panel de detalle de CashBoxDetail
    }

    private void updateCashBox() { //Cargar los atributos del CashBox Instanciado
        this.cashBox.setCashPartial(this.cashTotal);
        this.cashBox.setSaldoPartial(this.saldoCash);
        this.cashBox.setMissCashPartial(this.missingBreakdown.multiply(BigDecimal.valueOf(-1)));//Guardar valores positivos
        this.cashBox.setExcessCashPartial(this.excessBreakdown);
        if (this.cashBox.getId() != null) {
            this.cashBox.setLastUpdate(Dates.now());
        } else {
            this.cashBox.setAuthor(this.subject);
            this.cashBox.setOwner(this.subject);
            if (this.cashBox.getStatusCashBox() == null) {
                this.cashBox.setStatusCashBox(CashBox.Status.OPEN);
            }
            this.cashBox.setName(I18nUtil.getMessages("app.fede.accounting.close.cash") + " " + I18nUtil.getMessages("common.of") + " " + this.subject.getFirstname() + " " + this.subject.getSurname());
        }
    }

    private void updateCashBoxGeneral() { //Cargar los atributos del CashBoxGeneral Instanciado
        this.cashBoxGeneral.setSaldoFinal(BigDecimal.ZERO);
        this.cashBoxGeneral.setMissCashFinal(BigDecimal.ZERO);
        this.cashBoxGeneral.setExcessCashFinal(BigDecimal.ZERO);
        this.cashBoxGeneral.setCashFinal(this.cashTotal);
        this.cashBoxGeneral.setSaldoFinal(this.saldoCash);
        this.cashBoxGeneral.setMissCashFinal(this.missingBreakdown.multiply(BigDecimal.valueOf(-1)));
        this.cashBoxGeneral.setExcessCashFinal(this.excessBreakdown);
        this.cashBoxGeneral.setName(I18nUtil.getMessages("app.fede.accounting.close.cash") + " " + this.organizationData.getOrganization().getInitials() + "/" + Dates.toDateString(Dates.now()));
    }

    public void save() { //Guardar el CashBoxGeneral, con todos sus CashBoxs y CashBoxDetails
        updateCashBox(); //Cargar los atributos del CashBox Instanciado
        this.cashBoxGeneral.addCashBox(this.cashBox); //Agregar un CashBox al CashBoxGeneral
        updateCashBoxGeneral(); //Cargar los atributos del CashBoxGeneral Instanciado
        if (this.cashBoxGeneral.isPersistent()) {
            this.cashBoxGeneral.setLastUpdate(Dates.now());
        } else {
            this.cashBoxGeneral.setAuthor(this.subject);
            this.cashBoxGeneral.setOwner(this.subject);
            this.cashBoxGeneral.setStatusCashBoxGeneral(CashBoxGeneral.Status.OPEN);
            this.cashBoxGeneral.setAccount(this.selectedAccount);
            this.cashBoxGeneral.setOrganization(this.organizationData.getOrganization());
        }
        cashBoxGeneralService.save(this.cashBoxGeneral.getId(), this.cashBoxGeneral);
    }

    public void closeCashBoxChecker() {
        this.cashBox.setStatusCashBox(CashBox.Status.CLOSED); //Cambiar el estado del CashBox Instanciado (Cerrar)
        save(); //Guardar el CashBoxGeneral, con todos sus CashBoxs y CashBoxDetails
        this.activeSelectMenuBill = true; //Desabilitar los Selects del Panel de Detalle para Billetes y Monedas
        this.activeSelectMenuMoney = true;
        this.activeButtonCloseCash = false; //Habilitar el Button de CashBoxGeneral
    }

    public void closeCashBoxGeneral() {
        this.cashBoxGeneral.setStatusCashBoxGeneral(CashBoxGeneral.Status.CLOSED); //Cambiar el estado del CashBoxGeneral Instanciado (Cerrar)
        this.cashBoxGeneral.setAuthor(this.subject); //Actualiza, para saber que sujeto lo cierra por última vez
        cashBoxGeneralService.save(this.cashBoxGeneral.getId(), this.cashBoxGeneral); //Guardar el CashBoxGeneral, con todos sus CashBoxs y CashBoxDetails
        this.activePanelDeposit = false; //Ocultar el Panel de Depósito
    }

    public void validateAmount() { //Validar monto de depósito (Mensajes de validación)
        if (this.amountDeposit != null) {
            this.activeButtonSelectDeposit = !(this.amountDeposit.compareTo(BigDecimal.ZERO) == 1 && (this.amountDeposit.compareTo(this.saldoCash) == 0 || this.amountDeposit.compareTo(this.saldoCash) == -1)); //Activar/Desactivar Select y Botón de Depósito
            if (this.amountDeposit.compareTo(BigDecimal.ZERO) == 1) {
                if (this.amountDeposit.compareTo(this.saldoCash) == 1) {
                    this.addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.accouting.validate.deposit.amount.greater") + I18nUtil.getMessages("app.fede.accounting.ajust.money") + ": $" + this.saldoCash);
                }
            } else {
                this.addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.accouting.validate.deposit.amount.less.zero"));
            }
        }
    }

    public void validateDeposit() {
        if (this.depositAccount != null) {
            if (this.depositAccount.getId().equals(this.selectedAccount.getId())) {
                this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accouting.validate.deposit.account.equals"));
            } else {
                registerRecordInJournal(this.selectedAccount, this.depositAccount, this.amountDeposit); //Registar asiento contable
                calculeSummaryToday();
                cleanPanelDeposit(); //Resetear los valores del Panel de Depósito
            }
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accouting.validate.deposit.account"));
        }
    }

    public void openPanelBreakdown() { //Construir el Panel de Detalle
        this.activePanelBreakdown = true; //Mostrar el Panel de Detalle de CashBox Instanciado
        this.activeSelectDeposit = false; //Desactivar el Detalle del Panel de Depósito
        this.activePanelDeposit = false; //Ocultar el Panel de Depósito
        this.addInfoMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("common.start") + " " + I18nUtil.getMessages("app.fede.accounting.ajust.breakdown"));
    }

    public void cleanPanelDeposit() { //Resetear los valores del Panel de Depósito
        this.depositAccount = null;
        this.amountDeposit = BigDecimal.ZERO;
        this.activeButtonSelectDeposit = true; //Desahibiltar el Button y Select del Panel de Depósito
    }

    public void cleanPanelDetail() { //Resetear los inserts del Panel de Detalle de CashBoxDetail
        this.cashBoxDetail.setDenomination(null);
        this.cashBoxDetail.setQuantity(null);
        this.activeSelectMenuBill = false; //Habilitar los selects del Panel de Detalle para Billetes y Monedas
        this.activeSelectMenuMoney = false;
    }

    private void calculateValuerAndType() {
        switch (this.cashBoxDetail.getDenomination()) {
            case "$ 100":
                this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                this.cashBoxDetail.setValuer(BigDecimal.valueOf(100));
                break;
            case "$ 50":
                this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                this.cashBoxDetail.setValuer(BigDecimal.valueOf(50));
                break;
            case "$ 20":
                this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                this.cashBoxDetail.setValuer(BigDecimal.valueOf(20));
                break;
            case "$ 10":
                this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                this.cashBoxDetail.setValuer(BigDecimal.valueOf(10));
                break;
            case "$ 5":
                this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                this.cashBoxDetail.setValuer(BigDecimal.valueOf(5));
                break;
            case "$ 2":
                this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                this.cashBoxDetail.setValuer(BigDecimal.valueOf(2));
                break;
            case "$ 1 (billete)":
                this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                this.cashBoxDetail.setValuer(BigDecimal.valueOf(1));
                break;
            case "$ 1 (moneda)":
                this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.MONEY);
                this.cashBoxDetail.setValuer(BigDecimal.valueOf(1));
                break;
            case "50 ¢":
                this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.MONEY);
                this.cashBoxDetail.setValuer(BigDecimal.valueOf(0.50));
                break;
            case "25 ¢":
                this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.MONEY);
                this.cashBoxDetail.setValuer(BigDecimal.valueOf(0.25));
                break;
            case "10 ¢":
                this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.MONEY);
                this.cashBoxDetail.setValuer(BigDecimal.valueOf(0.10));
                break;
            case "5 ¢":
                this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.MONEY);
                this.cashBoxDetail.setValuer(BigDecimal.valueOf(0.05));
                break;
            case "1 ¢":
                this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.MONEY);
                this.cashBoxDetail.setValuer(BigDecimal.valueOf(0.01));
                break;
            default:
                break;
        }
    }

    //REGISTRO DE ASIENTOS CONTABLES
    public void registerRecordInJournal(Account selectedAccount, Account depositAccount, BigDecimal amountDeposit) { //Registar asiento contable
        //Crear/Buscar el journal y el record, donde insertar los recordDetails
        GeneralJournal journal = buildFindJournal();
        Record record = buildRecord();
        //Crear/Modificar un RecordDetail al Record del Journal del Día
        record.addRecordDetail(updateRecordDetail(selectedAccount, amountDeposit, RecordDetail.RecordTDetailType.HABER));
        record.addRecordDetail(updateRecordDetail(depositAccount, amountDeposit, RecordDetail.RecordTDetailType.DEBE));
        record.setDescription((I18nUtil.getMessages("app.fede.accounting.transfer.from") + selectedAccount.getName() + " " + I18nUtil.getMessages("common.to.a") + " " + depositAccount.getName()).toUpperCase());
        journal.addRecord(record);

        GeneralJournal save = journalService.save(journal.getId(), journal); //Validar la inserción del Record
        if (save != null) {
            this.addInfoMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("app.fede.accounting.record.sucessfully") + " a las: " + Dates.toTimeString(Dates.now()));
        } else {
            this.addInfoMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.record.fail"));
        }
    }

    private RecordDetail updateRecordDetail(Account account, BigDecimal amountDeposit, RecordDetail.RecordTDetailType type) {
        RecordDetail recordDetailGeneral = recordDetailService.createInstance();
        if (amountDeposit.compareTo(BigDecimal.ZERO) == 1) {
            recordDetailGeneral.setOwner(this.subject);
            recordDetailGeneral.setAmount(amountDeposit.abs());
            recordDetailGeneral.setAccount(account);
            if (type.equals(RecordDetail.RecordTDetailType.DEBE)) {
                recordDetailGeneral.setRecordDetailType(RecordDetail.RecordTDetailType.DEBE);
            } else if (type.equals(RecordDetail.RecordTDetailType.HABER)) {
                recordDetailGeneral.setRecordDetailType(RecordDetail.RecordTDetailType.HABER);
            }
        }
        return recordDetailGeneral;
    }

    private GeneralJournal buildFindJournal() {
        GeneralJournal generalJournal = journalService.findUniqueByNamedQuery("Journal.findByCreatedOnAndOrg", Dates.minimumDate(Dates.now()), Dates.now(), this.organizationData.getOrganization());
        if (generalJournal == null) {
            generalJournal = journalService.createInstance();
            generalJournal.setOrganization(this.organizationData.getOrganization());
            generalJournal.setOwner(this.subject);
            generalJournal.setCode(UUID.randomUUID().toString());
            generalJournal.setName(I18nUtil.getMessages("app.fede.accounting.journal") + " " + this.organizationData.getOrganization().getInitials() + "/" + Dates.toDateString(Dates.now()));
            journalService.save(generalJournal); //Guardar el journal creado
            generalJournal = journalService.findUniqueByNamedQuery("Journal.findByCreatedOnAndOrg", Dates.minimumDate(Dates.now()), Dates.now(), this.organizationData.getOrganization());
        }
        return generalJournal;
    }

    private Record buildRecord() {
        Record recordGeneral = recordService.createInstance();
        recordGeneral.setOwner(this.subject);
        return recordGeneral;
    }

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
