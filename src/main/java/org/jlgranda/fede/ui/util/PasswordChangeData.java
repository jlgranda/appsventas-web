/*
 * Copyright (C) 2016 jlgranda
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
package org.jlgranda.fede.ui.util;

import com.google.common.base.Strings;
import java.io.Serializable;

/**
 *
 * @author jlgranda
 */
public class PasswordChangeData implements Serializable {
    
    private String inputPassword;
    private String confirmPassword;

    public String getInputPassword() {
        return inputPassword;
    }

    public void setInputPassword(String inputPassword) {
        this.inputPassword = inputPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
    
    public boolean isValidPassworDataForChange(){
        if (Strings.isNullOrEmpty(this.getInputPassword())|| Strings.isNullOrEmpty(this.getConfirmPassword()))
            return false;
        
        return this.getInputPassword().equalsIgnoreCase(this.getConfirmPassword());
    }
    
}
