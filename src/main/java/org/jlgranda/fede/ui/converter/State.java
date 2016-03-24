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
package org.jlgranda.fede.ui.converter;

/**
 *
 * @author jlgranda
 */
import java.io.Serializable;

/**
 * State model class.
 *
 * @author  Mauricio Fenoglio / last modified by $Author:$
 * @version $Revision:$
 */
public class State implements Serializable {

	private String stateDesc;
	private String state;

	public State(String state) {
		this.state = state;
		this.stateDesc = "State class value = " + state;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getStateDesc() {
		return stateDesc;
	}

	public void setStateDesc(String stateDesc) {
		this.stateDesc = stateDesc;
	}

	@Override
	public String toString() {
		return stateDesc;
	}
}