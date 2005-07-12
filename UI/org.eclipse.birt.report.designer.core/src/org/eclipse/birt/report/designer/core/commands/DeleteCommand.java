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

package org.eclipse.birt.report.designer.core.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.designer.util.DNDUtil;
import org.eclipse.birt.report.designer.util.ImageManager;
import org.eclipse.birt.report.model.api.CellHandle;
import org.eclipse.birt.report.model.api.ColumnHandle;
import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.EmbeddedImageHandle;
import org.eclipse.birt.report.model.api.ListGroupHandle;
import org.eclipse.birt.report.model.api.ListHandle;
import org.eclipse.birt.report.model.api.MasterPageHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.ReportElementHandle;
import org.eclipse.birt.report.model.api.RowHandle;
import org.eclipse.birt.report.model.api.SlotHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.core.IStructure;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.viewers.StructuredSelection;

/**
 * Deletes single, multiple objects or do nothing.
 * 
 * 
 */

public class DeleteCommand extends Command
{

	private Object model = null;

	private ArrayList embeddedImageList = new ArrayList( );

	/**
	 * Deletes the command
	 * 
	 * @param model
	 *            the model
	 */

	public DeleteCommand( Object model )
	{
		this.model = model;
	}

	/**
	 * Executes the Command. This method should not be called if the Command is
	 * not executable.
	 */

	public void execute( )
	{
		try
		{
			dropSource( model );
			if ( !embeddedImageList.isEmpty( ) )
			{
				for ( int i = 0; i < embeddedImageList.size( ); i++ )
				{
					IStructure item = ( (EmbeddedImageHandle) embeddedImageList.get( i ) ).getStructure( );
					String name = ( (EmbeddedImageHandle) embeddedImageList.get( i ) ).getName( );
					SessionHandleAdapter.getInstance( )
							.getReportDesignHandle( )
							.getPropertyHandle( ReportDesignHandle.IMAGES_PROP )
							.removeItem( item );

					// remove cached image
					String key = ImageManager.getInstance( )
							.generateKey( SessionHandleAdapter.getInstance( )
									.getReportDesignHandle( ),
									name );
					ImageManager.getInstance( ).removeCachedImage( key );
				}
			}
		}
		catch ( SemanticException e )
		{
			e.printStackTrace( );
		}
	}

	protected void dropSource( Object source ) throws SemanticException
	{
		source = DNDUtil.unwrapToModel( source );
		if ( source instanceof Object[] )
		{
			Object[] array = (Object[]) source;
			for ( int i = 0; i < array.length; i++ )
			{
				dropSource( array[i] );
			}
		}
		else if ( source instanceof StructuredSelection )
		{
			dropSource( ( (StructuredSelection) source ).toArray( ) );
		}
		else if ( source instanceof DesignElementHandle )
		{
			dropSourceElementHandle( (DesignElementHandle) source );
		}
		else if ( source instanceof EmbeddedImageHandle )
		{
			dropEmbeddedImageHandle( (EmbeddedImageHandle) ( source ) );
		}
		else if ( source instanceof SlotHandle )
		{
			dropSourceSlotHandle( (SlotHandle) source );
		}
	}

	private void dropEmbeddedImageHandle( EmbeddedImageHandle embeddedImage )
	{
		embeddedImageList.add( embeddedImage );
	}

	protected void dropSourceElementHandle( DesignElementHandle handle )
			throws SemanticException
	{
		if ( handle.getContainer( ) != null )
		{
			if ( handle instanceof CellHandle )
			{
				dropSourceSlotHandle( ( (CellHandle) handle ).getContent( ) );
			}
			else if ( handle instanceof RowHandle )
			{
				new DeleteRowCommand( handle ).execute( );
			}
			else if ( handle instanceof ColumnHandle )
			{
				new DeleteColumnCommand( handle ).execute( );
			}
			else
			{
				handle.drop( );
			}
		}
	}

	protected void dropSourceSlotHandle( SlotHandle slot )
			throws SemanticException
	{
		List list = slot.getContents( );
		for ( int i = 0; i < list.size( ); i++ )
		{
			dropSourceElementHandle( (DesignElementHandle) list.get( i ) );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#canExecute()
	 */
	public boolean canExecute( )
	{
		return canDrop( model );
	}

	/**
	 * Returns the object can be deleted. If the parent can be deleted, the
	 * children will be skippedl
	 * 
	 * @param source
	 *            single or multiple objects
	 */
	protected boolean canDrop( Object source )
	{
		if ( source == null )
		{
			return false;
		}
		if ( source instanceof StructuredSelection )
		{
			return canDrop( ( (StructuredSelection) source ).toArray( ) );
		}
		if ( source instanceof Object[] )
		{
			Object[] array = (Object[]) source;
			if ( array.length == 0 )
			{
				return false;
			}
			// If the container can drop, the children will be skipped
			for ( int i = 0; i < array.length; i++ )
			{
				if ( DNDUtil.checkContainerExists( array[i], array ) )
					continue;
				if ( !canDrop( array[i] ) )
					return false;
			}
			return true;
		}
		source = DNDUtil.unwrapToModel( source );
		if ( source instanceof SlotHandle )
		{
			SlotHandle slot = (SlotHandle) source;
			DesignElementHandle handle = slot.getElementHandle( );
			return slot.getContents( ).size( ) > 0
					&& ( handle instanceof ListHandle || handle instanceof ListGroupHandle );
		}
		if ( source instanceof EmbeddedImageHandle )
		{
			return true;
		}
		if ( source instanceof CellHandle )
		{
			// CellHandle is subclass of ReportElementHandle
			return ( (CellHandle) source ).getContent( ).getContents( ).size( ) > 0;
		}
		return source instanceof ReportElementHandle
				&& !( source instanceof MasterPageHandle );
	}
}