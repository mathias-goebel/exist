/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-03 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist.sourceforge.net
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.xquery.value;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.exist.storage.Indexable;
import org.exist.util.ByteConversion;
import org.exist.xquery.XPathException;

/**
 * Represents a value of type xs:dateTime.
 * 
 * @author Wolfgang Meier (wolfgang@exist-db.org)
 * @author <a href="mailto:piotr@ideanest.com">Piotr Kaminski</a>
 */
public class DateTimeValue extends AbstractDateTimeValue implements Indexable {

	public DateTimeValue() throws XPathException {
		super(TimeUtils.getInstance().newXMLGregorianCalendar(new GregorianCalendar()));
	}
	
	public DateTimeValue(XMLGregorianCalendar calendar) {
		super(fillCalendar((XMLGregorianCalendar) calendar.clone()));
	}
	
	public DateTimeValue(Date date) {
		super(dateToXMLGregorianCalendar(date));
	}
	
	public DateTimeValue(String dateTime) throws XPathException {
		super(dateTime);
		try {
			if (calendar.getXMLSchemaType() != DatatypeConstants.DATETIME) throw new IllegalStateException();
		} catch (IllegalStateException e) {
			throw new XPathException("xs:dateTime instance must have all fields set");
		}
	}

	private static XMLGregorianCalendar dateToXMLGregorianCalendar(Date date) {
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(date);
		XMLGregorianCalendar xgc = TimeUtils.getInstance().newXMLGregorianCalendar(gc);
		xgc.normalize();
		return xgc;
	}
	
	private static XMLGregorianCalendar fillCalendar(XMLGregorianCalendar calendar) {
		if (calendar.getHour() == DatatypeConstants.FIELD_UNDEFINED) calendar.setHour(0);
		if (calendar.getMinute() == DatatypeConstants.FIELD_UNDEFINED) calendar.setMinute(0);
		if (calendar.getSecond() == DatatypeConstants.FIELD_UNDEFINED) calendar.setSecond(0);
		if (calendar.getMillisecond() == DatatypeConstants.FIELD_UNDEFINED) calendar.setMillisecond(0);
		return calendar;
	}

	protected AbstractDateTimeValue createSameKind(XMLGregorianCalendar cal) throws XPathException {
		return new DateTimeValue(cal);
	}

	protected QName getXMLSchemaType() {
		return DatatypeConstants.DATETIME;
	}

	public int getType() {
		return Type.DATE_TIME;
	}

	public AtomicValue convertTo(int requiredType) throws XPathException {
		switch (requiredType) {
			case Type.DATE_TIME :
			case Type.ATOMIC :
			case Type.ITEM :
				return this;
			case Type.DATE :
				return new DateValue(calendar);
			case Type.TIME :
				return new TimeValue(calendar);
			case Type.STRING :
				return new StringValue(getStringValue());
			default :
				throw new XPathException(
					"Type error: cannot cast xs:dateTime to "
						+ Type.getTypeName(requiredType));
		}
	}

	public ComputableValue minus(ComputableValue other) throws XPathException {
		switch(other.getType()) {
			case Type.DATE_TIME:
				return new DayTimeDurationValue(getTimeInMillis() - ((DateTimeValue) other).getTimeInMillis());
			case Type.YEAR_MONTH_DURATION:
				return ((YearMonthDurationValue) other).negate().plus(this);
			case Type.DAY_TIME_DURATION:
				return ((DayTimeDurationValue) other).negate().plus(this);
			default:
				throw new XPathException(
						"Operand to minus should be of type xs:dateTime, xdt:dayTimeDuration or xdt:yearMonthDuration; got: "
							+ Type.getTypeName(other.getType()));
		}
	}

	public Date getDate() { return calendar.toGregorianCalendar().getTime(); }
	
	
	/** @deprecated
     * @see org.exist.storage.Indexable#serialize(short)
     */
    public byte[] serialize(short collectionId, boolean caseSensitive)
    {
    	GregorianCalendar utccal = calendar.normalize().toGregorianCalendar();	//Get the dateTime (XMLGregorianCalendar) normalized to UTC (as a GregorianCalendar)
		long value = utccal.getTimeInMillis();									//Get the normalized dateTime as a long (milliseconds since the Epoch) 
        
		byte[] data = new byte[11];						//alocate a byte array for holding collectionId,Type,long (11 = (byte)short + byte + (byte)long)
		ByteConversion.shortToByte(collectionId, data, 0);	//put the collectionId in the byte array
		data[2] = (byte) Type.DATE_TIME;					//put the Type in the byte array
		ByteConversion.longToByte(value, data, 3);			//put the long in the byte array
		return(data);										//return the byte array
    }
    
    /** Serialize for the persistant storage */
    public byte[] serializeValue (int offset, boolean caseSensitive)
    {
    	GregorianCalendar utccal = calendar.normalize().toGregorianCalendar();	//Get the dateTime (XMLGregorianCalendar) normalized to UTC (as a GregorianCalendar)
		long value = utccal.getTimeInMillis();									//Get the normalized dateTime as a long (milliseconds since the Epoch) 
    	
		final byte[] data = new byte[offset + 1 + 8];		//allocate an appropriately sized byte array for holding Type,long
		data[offset] = (byte) Type.DATE_TIME;				//put the Type in the byte array
		ByteConversion.longToByte(value, data, offset+1);	//put the long into the byte array
		return(data);										//return the byte array
    }
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o)
    {
        final AtomicValue other = (AtomicValue)o;
        if(Type.subTypeOf(other.getType(), Type.DATE_TIME))
        	return calendar.compare((XMLGregorianCalendar)o);
        else
            return getType() > other.getType() ? 1 : -1;
    }

}
