package org.eclipse.jetty.uriqa;

import java.util.List;

import org.eclipse.jetty.util.log.Log;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelChangedListener;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * UriqaModelChangedListener implements {@link ModelChangedListener} <br />
 * The only implemented event is that of {@link UriqaModelChangedListener#notifyEvent(Model, Object)} <br />
 * TODO Can I do anything to rectify non-usage of these methods? Not using this. Why? Consider mput. Will have to check
 * validity here. And then rebind. If notValid, then would have to rollback. But cannot rollback here. Therefore, I'd
 * have to check for validity there again. Waste of computation.
 * 
 * @author venkatesh
 */
public class UriqaModelChangedListener implements ModelChangedListener
{

    public void addedStatement(Statement arg0)
    {

    }

    public void addedStatements(Statement[] arg0)
    {

    }

    public void addedStatements(List<Statement> arg0)
    {

    }

    public void addedStatements(StmtIterator arg0)
    {

    }

    public void addedStatements(Model arg0)
    {

    }

    /**
     * {@inheritDoc} notifyEvent(arg1) in this custom implementation of {@link UriqaModelChangedListener} assumes arg1
     * to be boolean and accordingly does a model.rebind() or not.
     * 
     * @param model Must be an instance of InfModel
     * @param arg1 Must be boolean
     * @see com.hp.hpl.jena.rdf.model.ModelChangedListener#notifyEvent(com.hp.hpl.jena.rdf.model.Model,
     *      java.lang.Object)
     */
    public void notifyEvent(Model model, Object arg1)
    {
        try {
            if ((Boolean) arg1 && model instanceof InfModel) {
                if (Log.isDebugEnabled())
                    Log.debug("notifyEvent(): rebinding");
                ((InfModel) model).rebind();
            }
        } catch (ClassCastException e) {
            /*
             * If not Boolean.
             */
        }
    }

    public void removedStatement(Statement arg0)
    {

    }

    public void removedStatements(Statement[] arg0)
    {

    }

    public void removedStatements(List<Statement> arg0)
    {

    }

    public void removedStatements(StmtIterator arg0)
    {

    }

    public void removedStatements(Model arg0)
    {

    }

}
