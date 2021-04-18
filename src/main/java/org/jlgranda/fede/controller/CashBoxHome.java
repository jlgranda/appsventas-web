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
    // Id de la Caja en Ajuste en edición
    private long cashBoxId;

    //Calcular resumen
    private BigDecimal grossSalesTotal;
    private BigDecimal discountTotal;
    private BigDecimal salesTotal;
    private BigDecimal purchasesTotal;
    private BigDecimal costTotal;
    private BigDecimal profilTotal;
    private Long paxTotal;
    private List<Object[]> listDiscount;

    //Calcular el cierre de caja: Monto de Depósito y desgloce de caja
    private boolean activeDeposit;
    private BigDecimal amountDeposit;
    private boolean validateAmountDeposit;
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
    private boolean activePanelDeposit;
    private boolean activeSelectMenuBill;
    private boolean activeSelectMenuMoney;

    //Desglosar el efectivo de Caja
    private CashBox cashBoxBills;//Almacena los cashBoxDetail de billetes 
    private CashBox cashBoxMoneys; //Almacena los cashBoxDetail de monedas 
    private BigDecimal subtotalBills;
    private BigDecimal subtotalMoneys;
    private BigDecimal totalBreakdown;

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
        setSelectedAccount(accountService.findUniqueByNamedQuery("Account.findByNameAndOrg", "CAJA", this.organizationData.getOrganization()));
        setGrossSalesTotal(BigDecimal.ZERO);
        setDiscountTotal(BigDecimal.ZERO);
        setSalesTotal(BigDecimal.ZERO);
        setPurchasesTotal(BigDecimal.ZERO);
        setCostTotal(BigDecimal.ZERO);
        setProfilTotal(BigDecimal.ZERO);
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
        setValidateAmountDeposit(true);
        setActivePanelDeposit(true);
        setActiveSelectMenuBill(false);
        setActiveSelectMenuMoney(false);
        setPaxTotal(0L);
        calculeSummaryToday();
        setCashBox(cashBoxService.createInstance());
        setCashBoxDetail(cashBoxDetailService.createInstance());
        setCashBoxBills(cashBoxService.createInstance());
        setCashBoxMoneys(cashBoxService.createInstance());
        setSubtotalBills(BigDecimal.ZERO);
        setSubtotalMoneys(BigDecimal.ZERO);
        setTotalBreakdown(BigDecimal.ZERO);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //GETTER AND SETTER
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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

    public long getCashBoxId() {
        return cashBoxId;
    }

    public void setCashBoxId(long cashBoxId) {
        this.cashBoxId = cashBoxId;
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

    public boolean isActiveDeposit() {
        return activeDeposit;
    }

    public void setActiveDeposit(boolean activeDeposit) {
        this.activeDeposit = activeDeposit;
    }

    public BigDecimal getAmountDeposit() {
        return amountDeposit;
    }

    public void setAmountDeposit(BigDecimal amountDeposit) {
        this.amountDeposit = amountDeposit;
    }

    public boolean isValidateAmountDeposit() {
        return validateAmountDeposit;
    }

    public void setValidateAmountDeposit(boolean validateAmountDeposit) {
        this.validateAmountDeposit = validateAmountDeposit;
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

    public boolean isActivePanelDeposit() {
        return activePanelDeposit;
    }

    public void setActivePanelDeposit(boolean activePanelDeposit) {
        this.activePanelDeposit = activePanelDeposit;
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
    
    public void activeSelectBill(){
        this.activeSelectMenuBill = true;
    }
    
    public void activeSelectMoney(){
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

    public BigDecimal getTotalBreakdown() {
        return totalBreakdown;
    }

    public void setTotalBreakdown(BigDecimal totalBreakdown) {
        this.totalBreakdown = totalBreakdown;
    }
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MÉTODOS/FUNCIONES
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //VENTAS/COMPRAS: SUMMARY
    public List<Object[]> getListDiscount(Date _start, Date _end) {
//        List<Object[]> objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceBussinesSalesDiscountBetween", Integer.MAX_VALUE, this.subject, DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end, BigDecimal.ZERO);
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
        calculeSummaryCashToday(_start, _end);
    }

    public void calculeSummary(Date _start, Date _end) {

        this.costTotal = BigDecimal.ZERO;
//        List<Object[]> objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesDiscountBetween", Integer.MAX_VALUE, this.subject, DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end);
        List<Object[]> objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesDiscountBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end);
        objects.stream().forEach((Object[] object) -> {
            this.grossSalesTotal = (BigDecimal) object[0];
            this.discountTotal = (BigDecimal) object[1];
            this.salesTotal = (BigDecimal) object[2];
        });
//        objects = invoiceService.findObjectsByNamedQueryWithLimit("FacturaElectronica.findTotalByEmissionTypeBetween", Integer.MAX_VALUE, this.subject, _start, _end, EmissionType.PURCHASE_CASH);
        objects = invoiceService.findObjectsByNamedQueryWithLimit("FacturaElectronica.findTotalByEmissionTypeBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), _start, _end, EmissionType.PURCHASE_CASH);
        objects.stream().forEach((Object object) -> {
            this.purchasesTotal = (BigDecimal) object;
        });
//        objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesPaxBetween", Integer.MAX_VALUE, this.subject, DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end);
        objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesPaxBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end);
        objects.stream().forEach((Object object) -> {
            this.paxTotal = (Long) object;
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

        if (this.purchasesTotal == null) {
            this.purchasesTotal = BigDecimal.ZERO;
        }

        if (this.paxTotal == null) {
            this.paxTotal = 0L;
        }
        this.profilTotal = this.salesTotal.subtract(this.purchasesTotal.add(this.costTotal));
    }

    //AJUSTE DE CIERRE DE CAJA
    public void calculeSummaryCashToday(Date _start, Date _end) {
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

        //Obtener el valor real de CAJA según los Libros
        //Total Ventas - Total Compras y la + o - de Transacciones
        this.cashTotal = this.salesTotal.subtract(this.purchasesTotal);
        this.cashTotal = this.cashTotal.add(this.transactionTotal);
    }

    public void validateDepositAmount() {
        if (this.amountDeposit != null) {
            if (this.amountDeposit.compareTo(BigDecimal.ZERO) == 1) {
                if (this.amountDeposit.compareTo(this.cashTotal) == 0 || this.amountDeposit.compareTo(this.cashTotal) == -1) {
                    setValidateAmountDeposit(false);
                } else {
                    this.addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.accouting.validate.deposit.amount.greater") + I18nUtil.getMessages("app.fede.accounting.account.fund.book") + ": $" + this.cashTotal);
                    setValidateAmountDeposit(true);
                }
            } else {
                this.addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.accouting.validate.deposit.amount.less.zero"));
                setValidateAmountDeposit(true);
            }
        } else {
            this.addWarningMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accouting.validate.deposit.amount.null"));
            setValidateAmountDeposit(true);
        }
    }

    public void validateDeposit() {
        if (this.depositAccount != null) {
            if (this.amountDeposit != null) {
                if (this.depositAccount.getId().equals(this.selectedAccount.getId())) {
                    this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accouting.validate.deposit.account.equals"));
                } else {
                    registerRecordInJournal(this.selectedAccount, this.depositAccount, this.amountDeposit);
                    calculeSummaryToday();
                    this.amountDeposit = null;
                }
            }
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accouting.validate.deposit.account"));
        }
    }

    public void validateBreakdown() {
        setActivePanelDeposit(false);
        setActiveDeposit(false);
        this.addInfoMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("common.start") + " " + I18nUtil.getMessages("app.fede.accounting.ajust.breakdown"));
        this.saldoCash = this.cashTotal.subtract(this.salesDedit.add(this.salesCredit));
    }

    /**
     * Agrega un detalle al CashBox
     */
    public void addCashDetail() {
        this.cashBoxDetail.setOwner(this.subject);
        this.cashBoxDetail = calculateValuerAmount(this.cashBoxDetail);
        this.cashBoxDetail.setAmount(this.cashBoxDetail.getValuer().multiply(BigDecimal.valueOf(this.cashBoxDetail.getQuantity())));
        if(this.cashBoxDetail.getDenomination_type().equals(CashBoxDetail.DenominationType.BILL)){ //Aregar los CashBoxDetails en la Data
            this.cashBoxBills.addCashBoxDetail(this.cashBoxDetail);
        }else if(this.cashBoxDetail.getDenomination_type().equals(CashBoxDetail.DenominationType.MONEY)){
            this.cashBoxMoneys.addCashBoxDetail(this.cashBoxDetail);
        }
        calculeSubtotals(this.cashBoxBills, this.cashBoxMoneys);
        //AGREGAR EL CASHBOXDETAIL EN LA INSTANCIA PRINCIPAL
        this.cashBox.addCashBoxDetail(this.cashBoxDetail);
        //Preparar para una nueva entrada
        this.cashBoxDetail = cashBoxDetailService.createInstance();
        //Habilitar los selectMenu de Billetes y Monedas
        this.activeSelectMenuBill = false;
        this.activeSelectMenuMoney = false;
    }
    
    private void calculeSubtotals(CashBox Bills, CashBox Moneys){
        this.subtotalBills = BigDecimal.ZERO;
        this.subtotalMoneys = BigDecimal.ZERO;
        Bills.getCashboxDetails().forEach((cashboxDetail) -> {
            this.subtotalBills = this.subtotalBills.add(cashboxDetail.getAmount());
        });
        Moneys.getCashboxDetails().forEach((cashboxDetail) -> {
            this.subtotalMoneys = this.subtotalMoneys.add(cashboxDetail.getAmount());
        });
    }

    private CashBoxDetail calculateValuerAmount(CashBoxDetail cashBoxDetail) {
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
                cashBoxDetail.setDenomination_type(null);
                cashBoxDetail.setValuer(BigDecimal.valueOf(0));
        }
        return cashBoxDetail;
    }
    
    public void cleanPanel(){
        //Resetear los valores del panel de CashBoxDetail
        System.out.println("\nthis.cashBoxDetail.setDenomination: "+this.cashBoxDetail.getDenomination());
        System.out.println("\nthis.cashBoxDetail.getquantity: "+this.cashBoxDetail.getQuantity());
        this.cashBoxDetail.setDenomination(null);
        this.cashBoxDetail.setQuantity(null);
        System.out.println("\nthis.cashBoxDetail.setDenomination: "+this.cashBoxDetail.getDenomination());
        System.out.println("\nthis.cashBoxDetail.getquantity: "+this.cashBoxDetail.getQuantity());
        this.activeSelectMenuBill=false;
        this.activeSelectMenuMoney=false;
    }

    //REGISTRO DE ASIENTOS CONTABLES
    public void registerRecordInJournal(Account selectedAccount, Account depositAccount, BigDecimal amountDeposit) {
        GeneralJournal journal = buildFindJournal(); //Crear o encontrar el journal y el record, para insertar los recordDetails
        Record record = buildRecord();
        record.addRecordDetail(updateRecordDetail(selectedAccount, amountDeposit, RecordDetail.RecordTDetailType.HABER)); //Crear/Modificar y Añadir un recordDetail al record
        record.addRecordDetail(updateRecordDetail(depositAccount, amountDeposit, RecordDetail.RecordTDetailType.DEBE));
        record.setDescription("Transferencia del valor de " + selectedAccount.getName() + " hacia " + depositAccount.getName());
        journal.addRecord(record); //Agregar el record al journal

        journalService.save(journal.getId(), journal); //Guardar el journal

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
