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
package org.jlgranda.fede.controller.talentohumano;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jlgranda
 */
public class JournalSummary {
    
    
    private Long totalDays;
    private Long normalDays;
    private Long sundayDays;
    List<Summary> days;

    public JournalSummary() {
        this.days = new ArrayList<>();
    }

    public Long getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(Long totalDays) {
        this.totalDays = totalDays;
    }

    public Long getNormalDays() {
        return normalDays;
    }

    public void setNormalDays(Long normalDays) {
        this.normalDays = normalDays;
    }

    public Long getSundayDays() {
        return sundayDays;
    }

    public void setSundayDays(Long sundayDays) {
        this.sundayDays = sundayDays;
    }

    public List<Summary> getDays() {
        return days;
    }

    public void setDays(List<Summary> days) {
        this.days = days;
    }

    void agregar(String name,Long count) {
        Summary summary = new Summary();
        summary.setName(name);
        summary.setAccount(count);
       this.days.add(summary);
    }

    private static class Summary {

        private String name;
        private Long account;
        public Summary() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getAccount() {
            return account;
        }

        public void setAccount(Long account) {
            this.account = account;
        }

        @Override
        public String toString() {
            return "Summary{" + "name=" + name + ", account=" + account + '}';
        }
        
        
        
    }

    @Override
    public String toString() {
        return "JournalSummary{" + "totalDays=" + totalDays + ", normalDays=" + normalDays + ", sundayDays=" + sundayDays + ", days=" + days + '}';
    }
    
    
    
}
