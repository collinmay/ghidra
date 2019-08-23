/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.app.plugin.core.symboltree.actions;

import javax.swing.tree.TreePath;

import docking.action.MenuData;
import docking.widgets.tree.GTree;
import docking.widgets.tree.GTreeNode;
import ghidra.app.plugin.core.symboltree.SymbolTreeActionContext;
import ghidra.app.plugin.core.symboltree.SymbolTreePlugin;
import ghidra.app.plugin.core.symboltree.nodes.ClassCategoryNode;
import ghidra.app.plugin.core.symboltree.nodes.SymbolNode;
import ghidra.app.util.NamespaceUtils;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.program.model.symbol.*;
import ghidra.util.Msg;
import ghidra.util.exception.DuplicateNameException;
import ghidra.util.exception.InvalidInputException;

public class ConvertToClassAction extends SymbolTreeContextAction {

	public ConvertToClassAction(SymbolTreePlugin plugin) {
		super("Convert to Class", plugin.getName());
		setPopupMenuData(new MenuData(new String[] { "Convert to Class" }, "0Create"));
		setEnabled(false);
	}

	@Override
	protected void actionPerformed(SymbolTreeActionContext context) {
		GTree tree = (GTree) context.getContextObject();
		if (tree.isFiltered()) {
			Msg.showWarn(getClass(), tree, "Convert to Class Not Allowed",
				"Cannot convert to class while the tree is filtered!");
			return;
		}
		convertToClass(context);
	}

	@Override
	protected boolean isEnabledForContext(SymbolTreeActionContext context) {

		TreePath[] selectionPaths = context.getSelectedSymbolTreePaths();
		if (selectionPaths.length == 1) {
			Object object = selectionPaths[0].getLastPathComponent();
			if (object instanceof ClassCategoryNode) {
				return true;
			}
			else if (object instanceof SymbolNode) {
				SymbolNode symbolNode = (SymbolNode) object;
				Symbol symbol = symbolNode.getSymbol();
				SymbolType symbolType = symbol.getSymbolType();
				if (symbolType == SymbolType.NAMESPACE) {
					// allow SymbolType to perform additional checks
					Namespace parentNamespace = (Namespace) symbol.getObject();
					return SymbolType.CLASS.isValidParent(context.getProgram(), parentNamespace,
						Address.NO_ADDRESS, parentNamespace.isExternal());
				}
			}
		}
		return false;
	}

	private void convertToClass(SymbolTreeActionContext context) {

		TreePath[] selectionPaths = context.getSelectedSymbolTreePaths();
		Program program = context.getProgram();
		Namespace parent = program.getGlobalNamespace();
		GTreeNode node = (GTreeNode) selectionPaths[0].getLastPathComponent();

		if (node instanceof SymbolNode) {
			Symbol symbol = ((SymbolNode) node).getSymbol();
			parent = (Namespace) symbol.getObject();
			if (parent == null) {
				return; // assume selected node has been deleted
			}
		}

		convertToClass(program, parent);

		program.flushEvents();
	}

	private void convertToClass(Program program, Namespace namespace) {
		int transactionID = program.startTransaction("Convert to Class");
		try {
			NamespaceUtils.convertNamespaceToClass(namespace);
		} catch (InvalidInputException e) {
			Msg.error(this, "Failed to convert to class", e);
		}
		finally {
			program.endTransaction(transactionID, true);
		}
	}
}
