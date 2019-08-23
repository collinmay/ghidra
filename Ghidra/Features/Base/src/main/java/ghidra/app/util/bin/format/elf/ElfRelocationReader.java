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

public abstract class ElfRelocationReader {
	protected long fileOffset = 0;
	protected long tableSize = 0;
	protected boolean hasBegun = false;
	
	/**
	 * GenericFactory construction and initialization method for a ELF relocation entry
	 * @param reader binary reader positioned at start of relocation entry.
	 * @param elfHeader ELF header
	 * @param relocationIndex index of entry in relocation table
	 * @return ELF relocation object
	 * @throws IOException
	 */
	ElfRelocation createElfRelocation(FactoryBundledWithBinaryReader reader,
			ElfHeader elfHeader, int relocationIndex) throws IOException {

		Class<? extends ElfRelocation> elfRelocationClass = ElfRelocation.getElfRelocationClass(elfHeader);
		ElfRelocation elfRelocation =
			(ElfRelocation) reader.getFactory().create(elfRelocationClass);
		elfRelocation.initElfRelocation(elfHeader, relocationIndex, hasAddend());
		readEntryData(reader, elfHeader, elfRelocation);
		return elfRelocation;
	}
	
	protected abstract boolean hasAddend();
	protected abstract void readEntryData(FactoryBundledWithBinaryReader reader, ElfHeader elfHeader, ElfRelocation elfRelocation) throws IOException;

	public void begin(FactoryBundledWithBinaryReader reader, long fileOffset, long tableSize) throws IOException {
		if(hasBegun) {
			throw new RuntimeException("relocation reader already begun");
		}
		reader.setPointerIndex(fileOffset);
		this.fileOffset = fileOffset;
		this.tableSize = tableSize;
		this.hasBegun = true;
	}
	
	public boolean hasMoreRelocations(FactoryBundledWithBinaryReader reader) {
		if(!hasBegun) {
			throw new RuntimeException("relocation reader not begun");
		}
		return reader.getPointerIndex() >= fileOffset + tableSize;
	}

	public boolean shouldMarkup() {
		return true;
	}
}
