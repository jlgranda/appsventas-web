/*
 * Copyright (C) 2021 jlgranda
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

import java.math.BigDecimal;
import org.jlgranda.fede.model.config.accounting.Account;
import org.jlgranda.fede.model.config.accounting.Journal;
import org.jlgranda.fede.model.config.accounting.Record;
import org.jlgranda.fede.model.config.accounting.RecordDetail;
import org.jpapi.util.Dates;

/**
 *
 * @author jlgranda
 */
public class AccountHome {
    
    
    public static void main(String[] args) {
        //La empresa adquiere papeleria 
        Account account1 = new Account();
        account1.setCode("1.1");
        account1.setName("Gastas de administración");
        
        Account account2 = new Account();
        account2.setCode("1.1");
        account2.setName("IVA POR ACREDITAR");
        
        Account account3 = new Account();
        account3.setCode("2.1");
        account3.setName("Acreedores diversos");
        
        Record record = new Record();
        record.setName("Registro ");
        record.setEmissionDate(Dates.now());
        
        RecordDetail recordDetail1 = new RecordDetail();
        recordDetail1.setAccount(account1);
        recordDetail1.setRecordType("DEBE");
        recordDetail1.setAmount(BigDecimal.valueOf(50000));
        
        RecordDetail recordDetail2 = new RecordDetail();
        recordDetail2.setAccount(account2);
        recordDetail2.setRecordType("DEBE");
        recordDetail2.setAmount(BigDecimal.valueOf(8000));
        
        RecordDetail recordDetail3 = new RecordDetail();
        recordDetail3.setAccount(account3);
        recordDetail3.setRecordType("HABER");
        recordDetail3.setAmount(BigDecimal.valueOf(58000));
        
        record.addRecordDetail(recordDetail1);
        record.addRecordDetail(recordDetail2);
        record.addRecordDetail(recordDetail3);
        
        
        Journal journal = new Journal();
        journal.setCreatedOn(Dates.now());
        journal.addRecord(record);
        
        System.out.println("Journal " + journal.getCreatedOn());
        System.out.println("Registro " + journal.getRecords().get(0).getName() + "-" + journal.getRecords().get(0).getEmissionDate());
        for (RecordDetail rd : journal.getRecords().get(0).getRecordDetails()){
            System.out.println(rd.getAccount().getName());
            System.out.println(rd.getRecordType());
            System.out.println(rd.getAmount());
        }
        
    }
    
}
