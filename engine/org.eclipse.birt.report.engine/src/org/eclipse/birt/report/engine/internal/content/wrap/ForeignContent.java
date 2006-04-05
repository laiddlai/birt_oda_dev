/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.engine.internal.content.wrap;

import org.eclipse.birt.report.engine.content.IContentVisitor;
import org.eclipse.birt.report.engine.content.IForeignContent;

public class ForeignContent extends AbstractContentWrapper
		implements
			IForeignContent
{

	IForeignContent foreignContent;

	public ForeignContent( IForeignContent content )
	{
		super( content );
		foreignContent = content;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.content.impl.AbstractContent#accept(org.eclipse.birt.report.engine.content.IContentVisitor)
	 */
	public void accept( IContentVisitor visitor, Object value )
	{
		visitor.visitForeign( this, value );
	}

	public String getRawType( )
	{
		return foreignContent.getRawType( );
	}

	/**
	 * @return Returns the content. Caller knows how to cast this object
	 */
	public Object getRawValue( )
	{
		return foreignContent.getRawValue( );
	}

	/**
	 * @param rawType
	 *            The rawType to set.
	 */
	public void setRawType( String rawType )
	{
		foreignContent.setRawType( rawType );
	}

	/**
	 * @param rawValue
	 *            The rawValue to set.
	 */
	public void setRawValue( Object rawValue )
	{
		foreignContent.setRawValue( rawValue );
	}
}
