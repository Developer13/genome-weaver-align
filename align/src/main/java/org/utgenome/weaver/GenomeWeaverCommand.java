/*--------------------------------------------------------------------------
 *  Copyright 2011 utgenome.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
//--------------------------------------
// genome-weaver Project
//
// GenomeWeaverCommand.java
// Since: 2011/04/25
//
// $URL$ 
// $Author$
//--------------------------------------
package org.utgenome.weaver;

import java.net.URL;

import org.xerial.util.opt.Argument;
import org.xerial.util.opt.Command;
import org.xerial.util.opt.GlobalCommandOption;
import org.xerial.util.opt.Option;

/**
 * A base class of genome-weaver commands. In order to add a new command, create
 * a class extending this class. Command line options can be specified by using
 * {@link Option} and {@link Argument} annotations to the class fields.
 * 
 * @author leo
 * 
 */
public abstract class GenomeWeaverCommand implements Command
{
    protected GlobalCommandOption globalOption = new GlobalCommandOption();

    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Object getOptionHolder() {
        return this;
    }

    @Override
    public URL getHelpMessageResource() {
        return null;
    }

    @Override
    public void execute(GlobalCommandOption globalOption, String[] args) throws Exception {
        this.globalOption = globalOption;
        execute(args);
    }

}
