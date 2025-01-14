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
package ghidra.app.plugin.core.decompile;

import ghidra.app.context.NavigatableActionContext;
import ghidra.app.context.RestrictedAddressSetContext;
import ghidra.app.decompiler.ClangToken;
import ghidra.app.decompiler.component.DecompilerPanel;
import ghidra.app.decompiler.component.DecompilerUtils;
import ghidra.program.model.address.Address;
import ghidra.program.util.ProgramLocation;

public class DecompilerActionContext extends NavigatableActionContext
		implements RestrictedAddressSetContext {
	private final Address functionEntryPoint;
	private final boolean isDecompiling;

	public DecompilerActionContext(DecompilerProvider provider, Address functionEntryPoint,
			boolean isDecompiling) {
		super(provider, provider);
		this.functionEntryPoint = functionEntryPoint;
		this.isDecompiling = isDecompiling;
	}

	public Address getFunctionEntryPoint() {
		return functionEntryPoint;
	}

	public boolean isDecompiling() {
		return isDecompiling;
	}

	@Override
	public DecompilerProvider getComponentProvider() {
		return (DecompilerProvider) super.getComponentProvider();
	}

	public DecompilerPanel getDecompilerPanel() {
		return getComponentProvider().getDecompilerPanel();
	}

	@Override
	public ProgramLocation getLocation() {

		// prefer the selection over the current location
		DecompilerPanel decompilerPanel = getDecompilerPanel();
		ClangToken token = decompilerPanel.getSelectedToken();
		if (token == null) {
			token = decompilerPanel.getTokenAtCursor();
		}

		if (token == null) {
			return null;
		}

		Address address = DecompilerUtils.getClosestAddress(program, token);
		if (address == null) {
			return null;
		}

		return new ProgramLocation(program, address);
	}
}
