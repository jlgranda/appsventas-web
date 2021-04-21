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
import com.jlgranda.fede.ejb.CashBoxService;
import com.jlgranda.fede.ejb.GeneralJournalService;
import com.jlgranda.fede.ejb.RecordDetailService;
import com.jlgranda.fede.ejb.RecordService;
import com.jlgranda.fede.ejb.sales.InvoiceService;
import java.io.Serializable;
import java.math.BigDecimal;
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
    private CashBoxService cashBoxService;

    @EJB
    private CashBoxDetailService cashBoxDetailService;

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
    private boolean activePanelButton;
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
    private CashBox cashBoxBills;//Almacena los cashBoxDetail de billetes 
    private CashBox cashBoxMoneys; //Almacena los cashBoxDetail de monedas
    private BigDecimal subtotalBills;
    private BigDecimal subtotalMoneys;
    private boolean activePanelBreakdown;
    private BigDecimal totalBreakdown;
    private BigDecimal missingBreakdown;
    private BigDecimal surplusBreakdown;

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
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * range)));
        setOutcome("close-cash");

        setCashBox(cashBoxService.createInstance());
        setCashBoxDetail(cashBoxDetailService.createInstance());

        setGrossSalesTotal(BigDecimal.ZERO);
        setDiscountTotal(BigDecimal.ZERO);
        setSalesTotal(BigDecimal.ZERO);
        setPurchasesTotal(BigDecimal.ZERO);
        setCostTotal(BigDecimal.ZERO);
        setProfilTotal(BigDecimal.ZERO);
        setPaxTotal(0L);

        setActivePanelButton(false); //rendered
        setActiveSelectDeposit(false); //rendered
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
        setActiveSelectMenuBill(false);
        setActiveSelectMenuMoney(false);

        setCashBoxBills(cashBoxService.createInstance());
        setCashBoxMoneys(cashBoxService.createInstance());
        setSubtotalBills(BigDecimal.ZERO);
        setSubtotalMoneys(BigDecimal.ZERO);
        setActivePanelBreakdown(false); //rendered
        setTotalBreakdown(BigDecimal.ZERO);
        setMissingBreakdown(BigDecimal.ZERO);
        setSurplusBreakdown(BigDecimal.ZERO);

        findCashBox();
        calculeSummaryToday();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //GETTER AND SETTER
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public CashBox getCashBox() {
        CashBox cashBoxGeneral = cashBoxService.findUniqueByNamedQuery("CashBox.findByCreatedOnAndOrg", Dates.minimumDate(Dates.now()), Dates.now(), this.organizationData.getOrganization());
        if(cashBoxGeneral!=null){
            this.cashBox = cashBoxGeneral;
        }
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

    public boolean isActivePanelButton() {
        return activePanelButton;
    }

    public void setActivePanelButton(boolean activePanelButton) {
        this.activePanelButton = activePanelButton;
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

    public CashBox getCashBoxBills() {
        return cashBoxBills;
    }

    public void setCashBoxBills(CashBox cashBoxBills) {
        this.cashBoxBills = cashBoxBills;
    }

    public CashBox getCashBoxMoneys() {
        return cashBoxMoneys;
    }

    public void setCashBoxMoneys(CashBox cashBoxMoneys) {
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

    public BigDecimal getSurplusBreakdown() {
        return surplusBreakdown;
    }

    public void setSurplusBreakdown(BigDecimal surplusBreakdown) {
        this.surplusBreakdown = surplusBreakdown;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MÉTODOS/FUNCIONES
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //VENTAS/COMPRAS: SUMMARY
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

    public void calculeSummaryToday() {
        Date _start = Dates.minimumDate(getEnd());
        Date _end = Dates.maximumDate(getEnd());
        calculeSummary(_start, _end);
        setListDiscount(getListDiscount(_start, _end));
        calculeSummaryCash(_start, _end);
    }

    public void calculeSummary(Date _start, Date _end) {
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

    //AJUSTE DE CIERRE DE CAJA
    private void findCashBox() {
        CashBox cashBoxGeneral = cashBoxService.findUniqueByNamedQuery("CashBox.findByCreatedOnAndOrg", Dates.minimumDate(Dates.now()), Dates.now(), this.organizationData.getOrganization());
        if(cashBoxGeneral!=null){
            this.cashBox = cashBoxGeneral;
            //Cargar los CashBoxDetails del CashBox existente
            for (CashBoxDetail cashboxDetail : this.cashBox.getCashboxDetails()){
                if(cashboxDetail.getDenomination_type().equals(CashBoxDetail.DenominationType.BILL)){
                    this.cashBoxBills.addCashBoxDetail(cashboxDetail);
                }else if(cashboxDetail.getDenomination_type().equals(CashBoxDetail.DenominationType.MONEY)){
                    this.cashBoxMoneys.addCashBoxDetail(cashboxDetail);
                }
            };
            this.cashTotal = this.cashBox.getAmountTotal();
            this.saldoCash = this.cashBox.getSaldoCash();
            calculateTotals(this.cashBoxBills, this.cashBoxMoneys);//Calcular los totales
        }
    }
    
    public void calculeSummaryCash(Date _start, Date _end) {
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

        this.salesTotal = this.salesCash.add(this.salesDedit).add(this.salesCredit); //Suma de tipo de ventas en caja
        this.purchasesTotal = this.purchasesCash.add(this.purchasesCredit);//Suma de tipo de compras en caja
        this.transactionTotal = this.transactionDebit.subtract(this.transactionCredit);//Suma de transacciones en caja
        //Obtener el valor real de CAJA según los Libros: Total Ventas - Total Compras y la + o - de Transacciones
        this.cashTotal = this.salesTotal.subtract(this.purchasesTotal).add(this.transactionTotal);
        this.saldoCash = this.cashTotal.subtract(this.salesDedit.add(this.salesCredit)); //Obtener sólo el dinero en efectivo
        
        //Activar paneles
        if(this.cashBox.getId()!=null){
            this.activePanelButton = false; //Activar/Desactivar Panel de Depósito y Botón de Desglose si ya se va a Editar un Cierre de Caja
            this.activePanelBreakdown = true; //Activar el Panel de Detalle que ya existe
        }else{
        this.activePanelButton = this.saldoCash.compareTo(BigDecimal.ZERO) == 1; //Activar/Desactivar Panel de Depósito y Botón de Desglose
        }
    }

    public void validateAmount() {
        if (this.amountDeposit != null) {
            this.activeButtonSelectDeposit = !(this.amountDeposit.compareTo(BigDecimal.ZERO) == 1 && (this.amountDeposit.compareTo(this.saldoCash) == 0 || this.amountDeposit.compareTo(this.saldoCash) == -1)); //Activar/Desactivar Select y Botón de Depósito
            if (this.amountDeposit.compareTo(BigDecimal.ZERO) == 1) { //Mensajes de validación
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
                registerRecordInJournal(this.selectedAccount, this.depositAccount, this.amountDeposit);
                calculeSummaryToday();
                cleanPanelDeposit();
            }
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accouting.validate.deposit.account"));
        }
    }

    public void cleanPanelDeposit() {
        this.depositAccount = null; //Resetear los valores del panel de Depósito
        this.amountDeposit = BigDecimal.ZERO;
        this.activeButtonSelectDeposit = true;
    }

    public void validPanelBreakdown() {
        this.activePanelBreakdown = true; //Abrir el desgloce
        this.activePanelButton = false; //Cerrar el Panel de depósito
        this.addInfoMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("common.start") + " " + I18nUtil.getMessages("app.fede.accounting.ajust.breakdown"));
    }

    public void cleanPanelDetail() {
        this.cashBoxDetail.setDenomination(null); //Resetear los valores del panel de detalle de CashBoxDetail
        this.cashBoxDetail.setQuantity(null);
        this.activeSelectMenuBill = false; //Activar los selectOneMenu
        this.activeSelectMenuMoney = false;
        for (CashBoxDetail cashboxDetail : this.cashBox.getCashboxDetails()) {
            System.out.print("\ncashboxDetail: "+cashboxDetail.getDenomination() +" -- " +cashboxDetail.getAmount());
        }
    }

    /**
     * ENTIDAD CASHBOX: Agrega un detalle al CashBox
     */
    public void addCashDetail() {
        this.cashBoxDetail.setAuthor(this.subject);
        this.cashBoxDetail.setOwner(this.subject);
        this.cashBoxDetail = calculateValuerAndType(this.cashBoxDetail); //Calcular el valor y tipo según la denominación
        this.cashBoxDetail.setAmount(this.cashBoxDetail.getValuer().multiply(BigDecimal.valueOf(this.cashBoxDetail.getQuantity())));
        if (this.cashBoxDetail.getDenomination_type().equals(CashBoxDetail.DenominationType.BILL)) { //Aregar los CashBoxDetails en la datatable
            this.cashBoxBills.addCashBoxDetail(this.cashBoxDetail);
        } else if (this.cashBoxDetail.getDenomination_type().equals(CashBoxDetail.DenominationType.MONEY)) {
            this.cashBoxMoneys.addCashBoxDetail(this.cashBoxDetail);
        }
        this.cashBox.addCashBoxDetail(this.cashBoxDetail);//Agregar el CashBoxDetail en la instancia principal
        for (CashBoxDetail cashboxDetail : this.cashBox.getCashboxDetails()) {
            System.out.print("\ncashboxDetail: "+cashboxDetail.getDenomination() +" -- " +cashboxDetail.getAmount());
        }

        calculateTotals(this.cashBoxBills, this.cashBoxMoneys); //Calcular el subtotal de billetes y monedas y total de efectivo
        for (CashBoxDetail cashboxDetail : this.cashBox.getCashboxDetails()) {
            System.out.print("\ncashboxDetail: "+cashboxDetail.getDenomination() +" -- " +cashboxDetail.getAmount());
        }
        this.cashBoxDetail = cashBoxDetailService.createInstance();//Preparar para una nueva entrada
        cleanPanelDetail(); //Resetear los valores del panel de detalle de CashBoxDetail

    }

    /**
     * ENTIDAD CASHBOX:Guardar el CashBoxPrincipal
     */
    public void save() {
        for (CashBoxDetail cashboxDetail : this.cashBox.getCashboxDetails()) {
            System.out.print("\ncashboxDetail: "+cashboxDetail.getDenomination() +" -- " +cashboxDetail.getAmount());
        }
        //Añadir attributos al CashBox Principal
        this.cashBox.setOrganization(this.organizationData.getOrganization());
        this.cashBox.setAccount(this.selectedAccount);
        this.cashBox.setName(I18nUtil.getMessages("app.fede.accounting.close.cash")+" " + I18nUtil.getMessages("common.of")+ " " + Dates.toDateString(Dates.now()));
        this.cashBox.setAmountTotal(this.cashTotal);
        this.cashBox.setSaldoCash(this.saldoCash);
        this.cashBox.setMissingCash(this.missingBreakdown);
        this.cashBox.setExcessCash(this.surplusBreakdown);
        if (this.cashBox.isPersistent()) {
            this.cashBox.setLastUpdate(Dates.now());
        } else {
            this.cashBox.setAuthor(this.subject);
            this.cashBox.setOwner(this.subject);
        }
        cashBoxService.save(this.cashBox.getId(), this.cashBox);
    }

    private void calculateTotals(CashBox Bills, CashBox Moneys) {
        this.subtotalBills = BigDecimal.ZERO;
        this.subtotalMoneys = BigDecimal.ZERO;
        this.missingBreakdown = BigDecimal.ZERO;
        this.surplusBreakdown = BigDecimal.ZERO;
        Bills.getCashboxDetails().forEach((cashboxDetail) -> {
            this.subtotalBills = this.subtotalBills.add(cashboxDetail.getAmount());
        });
        Moneys.getCashboxDetails().forEach((cashboxDetail) -> {
            this.subtotalMoneys = this.subtotalMoneys.add(cashboxDetail.getAmount());
        });
        this.totalBreakdown = this.subtotalBills.add(this.subtotalMoneys);
        switch (this.totalBreakdown.compareTo(this.saldoCash)) {
            case 0:
                this.missingBreakdown = BigDecimal.ZERO;
                this.surplusBreakdown = BigDecimal.ZERO;
                break;
            case -1:
                this.missingBreakdown = this.totalBreakdown.subtract(this.saldoCash);
                this.surplusBreakdown = BigDecimal.ZERO;
                break;
            case 1:
                this.surplusBreakdown = this.totalBreakdown.subtract(this.saldoCash);
                this.missingBreakdown = BigDecimal.ZERO;
                break;
            default:
                break;
        }
    }

    private CashBoxDetail calculateValuerAndType(CashBoxDetail cashBoxDetail) {
        switch (cashBoxDetail.getDenomination()) {
            case "$ 100":
                cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                cashBoxDetail.setValuer(BigDecimal.valueOf(100));
                break;
            case "$ 50":
                cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                cashBoxDetail.setValuer(BigDecimal.valueOf(50));
                break;
            case "$ 20":
                cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                cashBoxDetail.setValuer(BigDecimal.valueOf(20));
                break;
            case "$ 10":
                cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                cashBoxDetail.setValuer(BigDecimal.valueOf(10));
                break;
            case "$ 5":
                cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                cashBoxDetail.setValuer(BigDecimal.valueOf(5));
                break;
            case "$ 2":
                cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                cashBoxDetail.setValuer(BigDecimal.valueOf(2));
                break;
            case "$ 1 (billete)":
                cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                cashBoxDetail.setValuer(BigDecimal.valueOf(1));
                break;
            case "$ 1 (moneda)":
                cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.MONEY);
                cashBoxDetail.setValuer(BigDecimal.valueOf(1));
                break;
            case "50 ¢":
                cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.MONEY);
                cashBoxDetail.setValuer(BigDecimal.valueOf(0.50));
                break;
            case "25 ¢":
                cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.MONEY);
                cashBoxDetail.setValuer(BigDecimal.valueOf(0.25));
                break;
            case "10 ¢":
                cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.MONEY);
                cashBoxDetail.setValuer(BigDecimal.valueOf(0.10));
                break;
            case "5 ¢":
                cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.MONEY);
                cashBoxDetail.setValuer(BigDecimal.valueOf(0.05));
                break;
            case "1 ¢":
                cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.MONEY);
                cashBoxDetail.setValuer(BigDecimal.valueOf(0.01));
                break;
            default:
                break;
        }
        return cashBoxDetail;
    }

    //REGISTRO DE ASIENTOS CONTABLES
    public void registerRecordInJournal(Account selectedAccount, Account depositAccount, BigDecimal amountDeposit) {
        GeneralJournal journal = buildFindJournal(); //Crear o encontrar el journal y el record, para insertar los recordDetails
        Record record = buildRecord();
        record.addRecordDetail(updateRecordDetail(selectedAccount, amountDeposit, RecordDetail.RecordTDetailType.HABER)); //Crear/Modificar y Añadir un recordDetail al record
        record.addRecordDetail(updateRecordDetail(depositAccount, amountDeposit, RecordDetail.RecordTDetailType.DEBE));
        record.setDescription((I18nUtil.getMessages("app.fede.accounting.transfer.from") + selectedAccount.getName() + " " + I18nUtil.getMessages("common.to.a") + " " + depositAccount.getName()).toUpperCase());
        journal.addRecord(record); //Agregar el record al journal

        GeneralJournal save = journalService.save(journal.getId(), journal); //Guardar el journal
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
