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

package org.eclipse.birt.report.engine.api.impl;

import java.util.logging.Level;

import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.EngineException;
import org.eclipse.birt.report.engine.api.FORenderOption;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IRenderTask;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IReportView;
import org.eclipse.birt.report.engine.api.InstanceID;
import org.eclipse.birt.report.engine.api.ReportEngine;
import org.eclipse.birt.report.engine.emitter.EngineEmitterServices;
import org.eclipse.birt.report.engine.emitter.IContentEmitter;
import org.eclipse.birt.report.engine.executor.ReportExecutor;
import org.eclipse.birt.report.engine.extension.internal.ExtensionManager;
import org.eclipse.birt.report.engine.i18n.MessageConstants;
import org.eclipse.birt.report.engine.ir.Report;
import org.eclipse.birt.report.engine.parser.ReportParser;
import org.eclipse.birt.report.engine.presentation.LocalizedEmitter;
import org.eclipse.birt.report.engine.presentation.PageHint;
import org.eclipse.birt.report.engine.presentation.ReportContentLoader;
import org.eclipse.birt.report.engine.script.ReportContextImpl;

public class RenderTask extends EngineTask implements IRenderTask
{

	IRenderOption renderOptions;
	IReportView reportView;
	ReportDocument reportDoc;

	RenderTask( ReportEngine engine, IReportRunnable runnable,
			ReportDocument reportDoc )
	{
		super( engine, runnable );
		// load the reportR
		this.reportDoc = reportDoc;
		executionContext.setReportDocument( reportDoc );
		executionContext.setFactoryMode( false );
		executionContext.setPresentationMode( true );
		Report report = new ReportParser().parse( ((ReportRunnable)runnable).getReport() );
		executionContext.setReport(report);
		setParameterValues( reportDoc.getParameterValues( ) );
	}

	public void setRenderOption( IRenderOption options )
	{
		this.renderOptions = options;
	}

	public void setReportView( IReportView view )
	{
		this.reportView = view;
	}

	public IRenderOption getRenderOption( )
	{
		return renderOptions;
	}

	public void render( ) throws EngineException
	{
		render( null, null );
	}

	public void render( long pageNumber ) throws EngineException
	{
		PageHint pageHint = reportDoc.getPageHint( pageNumber );
		if ( pageHint == null )
		{
			throw new EngineException( "Can't find page hints :{0}", new Long(
					pageNumber ) );
		}
		InstanceID startId = InstanceID.parse( pageHint.getStartID( ) );
		InstanceID endId = InstanceID.parse( pageHint.getEndID( ) );
		/*
		 * if ( startId == null || endId == null ) { throw new EngineException(
		 * "Invalid instance id:{0} or {1}", new Object[]{pageHint.getStartID( ),
		 * pageHint.getEndID( )} ); }
		 */
		render( startId, endId );
	}

	public void render( String pageRange ) throws EngineException
	{
		throw new UnsupportedOperationException( "render" );
	}

	public void render( InstanceID iid ) throws EngineException
	{
		render( iid, iid );
	}

	protected void render( InstanceID siid, InstanceID eiid )
			throws EngineException
	{
		// create the emitter services object that is needed in the emitters.
		EngineEmitterServices services = new EngineEmitterServices( this );
		services.setRenderOption( renderOptions );

		EngineConfig config = engine.getConfig( );
		if ( config != null )
			services
					.setEmitterConfig( engine.getConfig( ).getEmitterConfigs( ) );
		services.setRenderContext( context );

		services.setReportRunnable( runnable );

		// register default parameters
		usingParameterValues( );

		// After setting up the parameter values and before executing the
		// report, we need to call onPrepare on all items.
		// Create IReportContext and set it to execution context
		ReportContextImpl reportContext = new ReportContextImpl(
				executionContext.getParams( ), config.getConfigMap( ),
				executionContext.getAppContext( ) );
		executionContext.setReportContext( reportContext );

		// setup runtime configurations
		// user defined configs are overload using system properties.
		executionContext.getConfigs( ).putAll( runnable.getTestConfig( ) );
		executionContext.getConfigs( ).putAll( System.getProperties( ) );

		// Set up rendering environment and check for supported format
		executionContext.setRenderOption( renderOptions );
		String format = renderOptions.getOutputFormat( );
		if ( format == null || format.length( ) == 0 ) // $NON-NLS-1
		{
			renderOptions.setOutputFormat( "html" ); // $NON-NLS-1
			format = "html"; // $NON-NLS-1
		}
		else if ( renderOptions != null && format.equalsIgnoreCase( "fo" ) // $NON-NLS-1
				&& ( (FORenderOption) renderOptions ).getTailoredForFOP( ) )
		{
			format = "fop"; // $NON-NLS-1
		}

		if ( !ExtensionManager.getInstance( ).getEmitterExtensions( )
				.containsKey( format ) )
		{
			log.log( Level.SEVERE,
					MessageConstants.FORMAT_NOT_SUPPORTED_EXCEPTION, format );
			throw new EngineException(
					MessageConstants.FORMAT_NOT_SUPPORTED_EXCEPTION, format );
		}

		IContentEmitter emitter = ExtensionManager.getInstance( )
				.createEmitter( format );
		if ( emitter == null )
		{
			log.log( Level.SEVERE, "Report engine can not create {0} emitter.",
					format ); // $NON-NLS-1$
			throw new EngineException(
					MessageConstants.CANNOT_CREATE_EMITTER_EXCEPTION );
		}

		ReportExecutor executor = new ReportExecutor( executionContext );
		services.setExecutor( executor );

		// localized emitter
		emitter = new LocalizedEmitter( executionContext, emitter );

		// emitter is not null
		emitter.initialize( services );

		try
		{
			ReportContentLoader loader = new ReportContentLoader(
					executionContext );
			loader.load( siid, eiid, emitter );
		}
		catch ( Exception ex )
		{
			log.log( Level.SEVERE,
					"An error happened while running the report. Cause:", ex ); //$NON-NLS-1$
		}
		catch ( OutOfMemoryError err )
		{
			log.log( Level.SEVERE,
					"An OutOfMemory error happened while running the report." ); //$NON-NLS-1$
			throw err;
		}
	}

}
