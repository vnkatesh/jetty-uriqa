package org.eclipse.jetty.uriqa;

import java.util.List;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelChangedListener;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class UriqaModelChangedListener implements ModelChangedListener {

	/*
	 * TODO Can I do anything to rectify non-usage of these methods?
	 * Not using this.
	 * Why?
	 * Consider mput.
	 * Will have to check validity here. And then rebind. If notValid, then would have to rollback.
	 * But cannot rollback here. Therefore, I'd have to check for validity there again. Waste of computation.
	 */
	public void addedStatement(Statement arg0) {

	}

	public void addedStatements(Statement[] arg0) {

	}

	public void addedStatements(List<Statement> arg0) {

	}

	public void addedStatements(StmtIterator arg0) {

	}

	public void addedStatements(Model arg0) {

	}

	public void notifyEvent(Model model, Object arg1) {
		try {
			if ((Boolean) arg1 && model instanceof InfModel) {
				((InfModel) model).rebind();
			}
		} catch (ClassCastException e) {
			// If Not Boolean.
		}
	}

	public void removedStatement(Statement arg0) {

	}

	public void removedStatements(Statement[] arg0) {

	}

	public void removedStatements(List<Statement> arg0) {

	}

	public void removedStatements(StmtIterator arg0) {

	}

	public void removedStatements(Model arg0) {

	}

}
