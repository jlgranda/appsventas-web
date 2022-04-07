/*
 * Copyright (C) 2022 usuario
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

import com.jlgranda.fede.ejb.CashBoxDetailService;
import com.jlgranda.fede.ejb.CashBoxGeneralService;
import com.jlgranda.fede.ejb.CashBoxPartialService;
import com.jlgranda.fede.ejb.GroupService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.model.accounting.CashBoxDetail;
import org.jlgranda.fede.model.accounting.CashBoxGeneral;
import org.jlgranda.fede.model.accounting.CashBoxPartial;
import org.jlgranda.fede.model.accounting.Record;
import org.jpapi.model.CodeType;
import org.jpapi.model.Group;
import org.jpapi.model.Setting;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author usuario
 */
@Named
@ViewScoped
public class CashBoxGeneralHome extends FedeController implements Serializable {
    
    @Inject
    private Subject subject;
    @Inject
    private OrganizationData organizationData;
    @Inject
    private SettingHome settingHome;
    
    @EJB
    private GroupService groupService;
    @EJB
    private CashBoxGeneralService cashBoxGeneralService;
    @EJB
    private CashBoxPartialService cashBoxPartialService;
    @EJB
    private CashBoxDetailService cashBoxDetailService;

    /**
     * EDIT OBJECT.
     */
    private Long cashBoxGeneralId;
    private CashBoxGeneral cashBoxGeneral;
    private CashBoxPartial cashBoxPartial;
    private CashBoxDetail cashBoxDetail;

    /**
     * UX.
     */
    //Calcular resumen
    private BigDecimal salesTotal = BigDecimal.ZERO;
    private BigDecimal purchasesTotal = BigDecimal.ZERO;
    private BigDecimal grossSalesTotal;
    private BigDecimal discountTotal;
    private BigDecimal costTotal;
    private BigDecimal profilTotal;
    private Long paxTotal;
    private List<Object[]> listDiscount;

    //Calcular el monto y panel de depósito
    private boolean activeButtonBreakdown;
    private boolean activePanelDeposit;
    private boolean activeSelectDeposit;
    private boolean activeButtonSelectDeposit;
    private Account selectedAccount;

    //Calcular el resumen del cierre de caja
    private BigDecimal salesCash = BigDecimal.ZERO;
    private BigDecimal salesCredit = BigDecimal.ZERO;
    private BigDecimal salesDedit = BigDecimal.ZERO;
    private BigDecimal salesCreditCollect = BigDecimal.ZERO;
    private BigDecimal purchasesCash = BigDecimal.ZERO;
    private BigDecimal purchasesCredit = BigDecimal.ZERO;
    private BigDecimal purchasesCreditPaid = BigDecimal.ZERO;
    private BigDecimal transactionDebit;
    private BigDecimal transactionCredit;
    private BigDecimal transactionTotal;

    //Obtener saldo inicial del cierre de caja anterior
    private BigDecimal saldoInitial = BigDecimal.ZERO;

    //Calcular el dinero obtenido del cierre de caja
    private BigDecimal cashTotal;
    private BigDecimal saldoCash = BigDecimal.ZERO;

    //Desglosar el efectivo de Caja
    private List<CashBoxDetail> cashBoxBills;
    private List<CashBoxDetail> cashBoxMoneys;
    private boolean activePanelBreakdown;

    //Obtener lista de CashBoxClosed
    private List<CashBoxPartial> cashBoxsClosed;
    private List<CashBoxDetail> cashBoxsClosedBills;
    private List<CashBoxDetail> cashBoxsClosedMoneys;
    private CashBoxPartial cashBoxOpen;
    private boolean activeButtonCloseCash;

    //Calcular el dinero restante luego del depósito del Cierre de Caja
    private BigDecimal saldoCashFund;
    private boolean activePanelBreakdownFund;
    private List<CashBoxDetail> cashBoxsInitialBills;
    private List<CashBoxDetail> cashBoxsInitialMoneys;
    private List<CashBoxGeneral> cashBoxGeneralInitial;
    private CashBoxPartial cashBoxInitialFinish;
    private int activeIndex;
    private boolean activePanelVerification;
    
    @PostConstruct
    private void init() {
        setCashBoxGeneral(cashBoxGeneralService.createInstance());
        getLastCashBoxGeneralId();
        setOutcome("cash-close");
    }
    
    public void getLastCashBoxGeneralId() {
        if (this.cashBoxGeneralId == null) {
            setCashBoxGeneralId(cashBoxGeneralService.findIdByLastCashBoxGeneral(this.organizationData.getOrganization()));
            getCashBoxGeneral();
        }
    }
    
    public Long getCashBoxGeneralId() {
        return cashBoxGeneralId;
    }
    
    public void setCashBoxGeneralId(Long cashBoxGeneralId) {
        this.cashBoxGeneralId = cashBoxGeneralId;
    }
    
    public CashBoxGeneral getCashBoxGeneral() {
        return cashBoxGeneral;
    }
    
    public void setCashBoxGeneral(CashBoxGeneral cashBoxGeneral) {
        this.cashBoxGeneral = cashBoxGeneral;
    }
    
    public CashBoxPartial getCashBoxPartial() {
        if (this.cashBoxGeneralId != null && !this.cashBoxGeneral.isPersistent()) {
            this.cashBoxGeneral = cashBoxGeneralService.find(this.cashBoxGeneralId);
        }
        return cashBoxPartial;
    }
    
    public void setCashBoxPartial(CashBoxPartial cashBoxPartial) {
        this.cashBoxPartial = cashBoxPartial;
    }
    
    public CashBoxDetail getCashBoxDetail() {
        return cashBoxDetail;
    }
    
    public void setCashBoxDetail(CashBoxDetail cashBoxDetail) {
        this.cashBoxDetail = cashBoxDetail;
    }

    /**
     * METHODS.
     */
    /**
     * METHODS UTIL.
     */
    public void initBreakdown() {
        setActivePanelBreakdown(true); //Mostrar el Panel de Detalle de CashBox instanciado
        setActivePanelDeposit(false); //Ocultar el Panel de Depósito mientras se detalla
        setActiveButtonBreakdown(true); //Deshabilitar el botón de inicio de desglose
        setActiveButtonCloseCash(true); //Deshabilitar el botón de finalización de caja del día
        setActiveIndex(-1); //Minimizar el Panel de Último CashBoxPartial cerrado
        generateCashBoxPartialDetails();
        this.addInfoMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("common.start") + " " + I18nUtil.getMessages("app.fede.accounting.ajust.breakdown"));
        
    }
    
    private void generateCashBoxPartialDetails() {
        List<Setting> denominations = settingHome.findByCodeType(CodeType.DENOMINATION);
        Collections.sort(denominations, (Setting sett, Setting other) -> sett.getCategory().compareTo(other.getCategory())); //Ordenar la lista de denominaciones según su tipo
        for (int i = (denominations.size() - 1); i >= 0; i--) {
            for (int j = 1; j <= i; j++) {
                if ((new BigDecimal(denominations.get(j - 1).getValue())).compareTo(new BigDecimal(denominations.get(j).getValue())) == -1
                        && denominations.get(j - 1).getCategory().equals(denominations.get(j).getCategory())) {
                    Setting temp = denominations.get(j - 1);
                    denominations.set((j - 1), denominations.get(j));
                    denominations.set(j, temp);
                }
            }
        }
        if (this.cashBoxPartial.getCashBoxDetails().isEmpty()) {
            denominations.stream().map(d -> {
                this.cashBoxDetail = cashBoxDetailService.createInstance(); //Preparar para un nuevo detalle del CashBox instanciado
                return d;
            }).map(d -> {
                if (d.getCategory().equals(CashBoxDetail.DenominationType.BILL.toString())) {
                    this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                } else if (d.getCategory().equals(CashBoxDetail.DenominationType.MONEY.toString())) {
                    this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.MONEY);
                }
                return d;
            }).map(d -> {
                this.cashBoxDetail.setDenomination(d.getLabel());
                return d;
            }).map(d -> {
                this.cashBoxDetail.setQuantity(0L);
                return d;
            }).map(d -> {
                this.cashBoxDetail.setValuer(new BigDecimal(d.getValue()));
                return d;
            }).map(_item -> {
                this.cashBoxDetail.setAmount(this.cashBoxDetail.getValuer().multiply(BigDecimal.valueOf(this.cashBoxDetail.getQuantity())));
                return _item;
            }).forEachOrdered(_item -> {
                this.cashBoxPartial.addCashBoxDetail(this.cashBoxDetail);//Agregar el CashBoxDetail al CashBox instanciado
            });
        }
        chargeListCashBoxBillMoney();
        if (this.cashBoxGeneral.getId() == null) {
            this.cashBoxPartial.setPriority(0);
        } else {
            this.cashBoxPartial.setPriority((int) cashBoxPartialService.count("CashBoxPartial.countCashBoxPartialByCashBoxGeneral", this.cashBoxGeneral));
        }
        if (this.cashBoxPartial.getPriority_order() == null) {
            this.cashBoxPartial.setPriority_order(CashBoxPartial.Priority.MAIN);
        }
    }
    
    private void chargeListCashBoxBillMoney() {
        this.cashBoxBills = new ArrayList<>();
        this.cashBoxMoneys = new ArrayList<>();
        this.cashBoxPartial.getCashBoxDetails().forEach(cashboxDetail -> {
            if (cashboxDetail.getDenomination_type().equals(CashBoxDetail.DenominationType.BILL)) {
                this.cashBoxBills.add(cashboxDetail);
            } else if (cashboxDetail.getDenomination_type().equals(CashBoxDetail.DenominationType.MONEY)) {
                this.cashBoxMoneys.add(cashboxDetail);
            }
        });
    }
    
    public void closeCashBoxGeneral() {
        this.cashBoxGeneral.setStatusCashBoxGeneral(CashBoxGeneral.Status.CLOSED); //Cambiar el estado del CashBoxGeneral instanciado (Cerrar)
        cashBoxGeneralService.save(this.cashBoxGeneral.getId(), this.cashBoxGeneral); //Guardar el CashBoxGeneral, con todos sus CashBoxs y CashBoxDetails
        setActiveButtonCloseCash(true);
        setActiveButtonBreakdown(true);
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
    
    public boolean isActiveButtonBreakdown() {
        return activeButtonBreakdown;
    }
    
    public void setActiveButtonBreakdown(boolean activeButtonBreakdown) {
        this.activeButtonBreakdown = activeButtonBreakdown;
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
    
    public BigDecimal getSalesCreditCollect() {
        return salesCreditCollect;
    }
    
    public void setSalesCreditCollect(BigDecimal salesCreditCollect) {
        this.salesCreditCollect = salesCreditCollect;
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
    
    public BigDecimal getPurchasesCreditPaid() {
        return purchasesCreditPaid;
    }
    
    public void setPurchasesCreditPaid(BigDecimal purchasesCreditPaid) {
        this.purchasesCreditPaid = purchasesCreditPaid;
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
    
    public BigDecimal getSaldoInitial() {
        return saldoInitial;
    }
    
    public void setSaldoInitial(BigDecimal saldoInitial) {
        this.saldoInitial = saldoInitial;
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
    
    public boolean isActivePanelBreakdown() {
        return activePanelBreakdown;
    }
    
    public void setActivePanelBreakdown(boolean activePanelBreakdown) {
        this.activePanelBreakdown = activePanelBreakdown;
    }
    
    public List<CashBoxPartial> getCashBoxsClosed() {
        return cashBoxsClosed;
    }
    
    public void setCashBoxsClosed(List<CashBoxPartial> cashBoxsClosed) {
        this.cashBoxsClosed = cashBoxsClosed;
    }
    
    public List<CashBoxDetail> getCashBoxsClosedBills() {
        return cashBoxsClosedBills;
    }
    
    public void setCashBoxsClosedBills(List<CashBoxDetail> cashBoxsClosedBills) {
        this.cashBoxsClosedBills = cashBoxsClosedBills;
    }
    
    public List<CashBoxDetail> getCashBoxsClosedMoneys() {
        return cashBoxsClosedMoneys;
    }
    
    public void setCashBoxsClosedMoneys(List<CashBoxDetail> cashBoxsClosedMoneys) {
        this.cashBoxsClosedMoneys = cashBoxsClosedMoneys;
    }
    
    public CashBoxPartial getCashBoxOpen() {
        return cashBoxOpen;
    }
    
    public void setCashBoxOpen(CashBoxPartial cashBoxOpen) {
        this.cashBoxOpen = cashBoxOpen;
    }
    
    public boolean isActiveButtonCloseCash() {
        return activeButtonCloseCash;
    }
    
    public void setActiveButtonCloseCash(boolean activeButtonCloseCash) {
        this.activeButtonCloseCash = activeButtonCloseCash;
    }
    
    public BigDecimal getSaldoCashFund() {
        return saldoCashFund;
    }
    
    public void setSaldoCashFund(BigDecimal saldoCashFund) {
        this.saldoCashFund = saldoCashFund;
    }
    
    public boolean isActivePanelBreakdownFund() {
        return activePanelBreakdownFund;
    }
    
    public void setActivePanelBreakdownFund(boolean activePanelBreakdownFund) {
        this.activePanelBreakdownFund = activePanelBreakdownFund;
    }
    
    public List<CashBoxDetail> getCashBoxsInitialBills() {
        return cashBoxsInitialBills;
    }
    
    public void setCashBoxsInitialBills(List<CashBoxDetail> cashBoxsInitialBills) {
        this.cashBoxsInitialBills = cashBoxsInitialBills;
    }
    
    public List<CashBoxDetail> getCashBoxsInitialMoneys() {
        return cashBoxsInitialMoneys;
    }
    
    public void setCashBoxsInitialMoneys(List<CashBoxDetail> cashBoxsInitialMoneys) {
        this.cashBoxsInitialMoneys = cashBoxsInitialMoneys;
    }
    
    public List<CashBoxGeneral> getCashBoxGeneralInitial() {
        return cashBoxGeneralInitial;
    }
    
    public void setCashBoxGeneralInitial(List<CashBoxGeneral> cashBoxGeneralInitial) {
        this.cashBoxGeneralInitial = cashBoxGeneralInitial;
    }
    
    public CashBoxPartial getCashBoxInitialFinish() {
        return cashBoxInitialFinish;
    }
    
    public void setCashBoxInitialFinish(CashBoxPartial cashBoxInitialFinish) {
        this.cashBoxInitialFinish = cashBoxInitialFinish;
    }
    
    public int getActiveIndex() {
        return activeIndex;
    }
    
    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }
    
    public boolean isActivePanelVerification() {
        return activePanelVerification;
    }
    
    public void setActivePanelVerification(boolean activePanelVerification) {
        this.activePanelVerification = activePanelVerification;
    }
    
    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
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
