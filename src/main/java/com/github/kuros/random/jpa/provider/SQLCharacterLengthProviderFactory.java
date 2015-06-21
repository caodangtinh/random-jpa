package com.github.kuros.random.jpa.provider;

import com.github.kuros.random.jpa.Database;
import com.github.kuros.random.jpa.provider.mssql.MSSQLCharacterLengthProvider;

/*
 * Copyright (c) 2015 Kumar Rohit
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License or any
 *    later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class SQLCharacterLengthProviderFactory {

    public static SQLCharacterLengthProvider getSqlCharacterLengthProvider(final Database database) {
        SQLCharacterLengthProvider sqlCharacterLengthProvider;
        switch (database) {
            case MS_SQL_SERVER:
                sqlCharacterLengthProvider = MSSQLCharacterLengthProvider.getInstance();
                break;
            default:
                sqlCharacterLengthProvider = new DefaultSQLCharacterLengthProvider();
                break;
        }

        return sqlCharacterLengthProvider;
    }

    public static class DefaultSQLCharacterLengthProvider implements SQLCharacterLengthProvider {
        public Integer getMaxLength(final String entityName, final String attributeName) {
            return null;
        }
    }
}