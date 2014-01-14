package com.buildml.eclipse.utils.handlers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import com.buildml.eclipse.utils.errors.FatalError;

/**
 * An abstract Eclipse command handler that wraps AbstractHandler, but provides
 * support for IProgressService. This base class should be used by any Eclipse command
 * handler that might take a long time.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public abstract class AbstractHandlerWithProgress extends AbstractHandler {
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * A wrapper around the standard execute() method for an Eclipse command handler. This
	 * method fires up a background thread that is run within the Eclipse progress service.
	 */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {

		final String cmdName = getCommandName() + "...";
		
		/* run the command handler within the Eclipse progress service */
		IProgressService service = PlatformUI.getWorkbench().getProgressService();
		try {			
			service.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					
					/* run the standard command handler as a Job that is monitored */
					monitor.beginTask(cmdName, IProgressMonitor.UNKNOWN);
					Job deleteJob = new Job(cmdName){
						@Override
						public IStatus run(IProgressMonitor monitor) {
							executeWithProgress(event);
							return Status.OK_STATUS;
						}
					};
					deleteJob.schedule();
					deleteJob.join();
					monitor.done();
				}
			});
			
		} catch (InvocationTargetException e) {
			throw new FatalError("Interrupted during execution of Eclipse command handler");
		} catch (InterruptedException e) {
			return null;
		}
		
		return null;
	}
	
	/*=====================================================================================*
	 * ABSTRACT METHODS
	 *=====================================================================================*/

	/**
	 * This is a replacement for the standard execute() method that all Eclipse command handlers
	 * must have. All command handlers that implement {@link AbstractHandlerWithProgress} must
	 * rename their execute() method to be executeWithProgress(). This method will be executed
	 * in a non-UI thread.
	 * 
	 * @param event	The event passed in by the Eclipse command handler framework.
	 * @return Always null.
	 */
	public abstract Object executeWithProgress(ExecutionEvent event);
	
	/**
	 * @return A user-facing string that will be shown on the progress dialog.
	 */
	public abstract String getCommandName();
	
	/*-------------------------------------------------------------------------------------*/
}
