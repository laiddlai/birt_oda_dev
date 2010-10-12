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

package org.eclipse.birt.report.engine.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.eclipse.birt.report.engine.EngineCase;

public class EngintTaskLoggerTest extends EngineCase
{

	public void testLogger( ) throws Exception
	{
		Logger engineLogger = Logger.getAnonymousLogger( );
		ByteArrayOutputStream engineOut = new ByteArrayOutputStream( );
		StreamHandler engineHandler = new StreamHandler( engineOut,
				new SimpleFormatter( ) );
		engineHandler.setLevel( Level.ALL );
		engineLogger.addHandler( engineHandler );
		engineLogger.setLevel( Level.ALL );

		Logger taskLogger = Logger.getAnonymousLogger( );
		ByteArrayOutputStream taskOut = new ByteArrayOutputStream( );
		StreamHandler taskHandler = new StreamHandler( taskOut,
				new SimpleFormatter( ) );
		taskHandler.setLevel( Level.ALL );
		taskLogger.addHandler( taskHandler );
		taskLogger.setLevel( Level.ALL );

		engine.setLogger( engineLogger );
		engine.changeLogLevel( Level.ALL );

		engineHandler.flush( );
		taskHandler.flush( );

		new File( "./utest/" ).mkdirs( );
		copyResource(
				"org/eclipse/birt/report/engine/api/engine-task-logger-test.rptdesign",
				"./utest/reportdesign.rptdesign" );
		IReportRunnable runnable = engine
				.openReportDesign( "./utest/reportdesign.rptdesign" );

		IRunTask task = engine.createRunTask( runnable );
		task.setParameter( "sample", "==golden values==", "displayText" );
		task.run( "./utest/report.rptdocument" );
		task.close( );

		// the logger is output to engine out.
		engineHandler.flush( );
		assertTrue( engineOut.toString( ).indexOf( "==golden values==" ) != -1 );
		engineOut.reset( );

		task = engine.createRunTask( runnable );
		task.setLogger( taskLogger );
		task.setParameter( "sample", "==golden values==", "displayText" );
		task.run( "./utest/report.rptdocument" );
		task.close( );

		engineHandler.flush( );
		taskHandler.flush( );
		assertTrue( engineOut.toString( ).indexOf( "==golden values==" ) == -1 );
		assertTrue( taskOut.toString( ).indexOf( "==golden values==" ) != -1 );
	}
}
