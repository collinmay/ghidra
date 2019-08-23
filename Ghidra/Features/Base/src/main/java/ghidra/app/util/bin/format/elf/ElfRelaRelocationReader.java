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
package ghidra.app.util.bin.format.elf;

import java.io.IOException;

import ghidra.app.util.bin.format.FactoryBundledWithBinaryReader;
import ghidra.util.Conv;

public class ElfRelaRelocationReader extends ElfRelocationReader {

	@Override
	protected boolean hasAddend() {
		return true;
	}

	@Override
	protected void readEntryData(FactoryBundledWithBinaryReader reader, ElfHeader elfHeader,
			ElfRelocation rel) throws IOException {
		long r_offset;
		long r_info;
		long r_addend;
		if (rel.is32Bit()) {
			r_offset = reader.readNextInt() & Conv.INT_MASK;
			r_info = reader.readNextInt() & Conv.INT_MASK;
			r_addend = reader.readNextInt() & Conv.INT_MASK;
		}
		else {
			r_offset = reader.readNextLong();
			r_info = reader.readNextLong();
			r_addend = reader.readNextLong();
		}
		
		rel.supplyEntryData(r_offset, r_info, r_addend);
	}
}
