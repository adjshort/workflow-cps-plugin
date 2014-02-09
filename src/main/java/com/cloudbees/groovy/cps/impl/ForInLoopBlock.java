package com.cloudbees.groovy.cps.impl;

import com.cloudbees.groovy.cps.Block;
import com.cloudbees.groovy.cps.Continuation;
import com.cloudbees.groovy.cps.Env;
import com.cloudbees.groovy.cps.Next;
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter;

import java.util.Iterator;

/**
 * for (Type var in col) { ... }
 *
 * @author Kohsuke Kawaguchi
 */
public class ForInLoopBlock implements Block {
    final Class type;
    final String variable;
    final Block collection;
    final Block body;

    public ForInLoopBlock(Class type, String variable, Block collection, Block body) {
        this.type = type;
        this.variable = variable;
        this.collection = collection;
        this.body = body;
    }

    public Next eval(Env e, Continuation k) {
        ContinuationImpl c = new ContinuationImpl(e, k);
        return c.then(collection, e, loopHead);
    }

    class ContinuationImpl extends ContinuationGroup {
        final Continuation loopEnd;
        final Env e;

        Iterator itr;

        ContinuationImpl(Env _e, Continuation loopEnd) {
            this.e = new BlockScopeEnv(_e);
            this.e.declareVariable(type,variable);
            this.loopEnd = loopEnd;
        }

        public Next loopHead(Object col) {
            try {
                itr = (Iterator) ScriptBytecodeAdapter.invokeMethod0(null/*unused*/, col, "iterator");
            } catch (Throwable e) {
                // TODO: exception handling
                e.printStackTrace();
                return loopEnd.receive(null);
            }

            return increment(null);
        }

        public Next increment(Object _) {
            if (itr.hasNext()) {
                // one more iteration
                e.setLocalVariable(variable,itr.next());
                return then(body,e,increment);
            } else {
                // exit loop
                return loopEnd.receive(null);
            }
        }
    }

    static final ContinuationPtr loopHead = new ContinuationPtr(ContinuationImpl.class,"loopHead");
    static final ContinuationPtr increment = new ContinuationPtr(ContinuationImpl.class,"increment");
}
