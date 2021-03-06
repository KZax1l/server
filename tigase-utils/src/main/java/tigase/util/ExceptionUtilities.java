/*
 * ExceptionUtilities.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */

package tigase.util;

import java.util.Collections;

public class ExceptionUtilities {

	public static String getExceptionRootCause(Exception e, boolean includeSource) {
		StringBuffer sb = new StringBuffer("\nRootCause:");
		Throwable cause = e;
		int idx = 1;
		while (cause.getCause() != null) {
			cause = cause.getCause();
			final String indent = String.join("", Collections.nCopies(idx++, "   "));
			sb.append("\n").append(indent).append("-> ").append(cause);
			if (includeSource) {
				sb.append("\n").append(indent).append("   [").append(cause.getStackTrace()[0]).append("]");
			}
		}
		return sb.toString();
	}
}
