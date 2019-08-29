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

public class ElfAPS2RelocationReader extends ElfRelocationReader {
	private long totalCount = 0;
	private long relocationIndex = 0;
	private long relocationGroupIndex = 0;
	private long groupSize = 0;
	private long groupFlags = 0;
	private long groupROffsetDelta = 0;
	private long r_offset = 0;
	private long r_info = 0;
	private long r_addend = 0;
	private boolean hasAddend;

	public ElfAPS2RelocationReader(boolean hasAddend) {
		this.hasAddend = hasAddend;
	}

	@Override
	protected boolean hasAddend() {
		return hasAddend;
	}

	public boolean hasMoreRelocations(FactoryBundledWithBinaryReader reader) {
		if(!hasBegun) {
			throw new RuntimeException("relocation reader not begun");
		}
		return relocationIndex < totalCount;
	}
	
	@Override
	protected void readEntryData(FactoryBundledWithBinaryReader reader, ElfHeader elfHeader,
			ElfRelocation elfRelocation) throws IOException {
		if(relocationGroupIndex == groupSize) {
			readGroupFields(reader);
		}
		
		if(isRelocationGroupedByOffsetDelta()) {
			r_offset+= groupROffsetDelta;
		} else {
			r_offset+= reader.readNextSLEB128();
		}
		
		if(!isRelocationGroupedByInfo()) {
			r_info = reader.readNextSLEB128();
		}
		
		if(hasAddend) {
			if(isRelocationGroupHasAddend() &&
					!isRelocationGroupedByAddend()) {
				r_addend+= reader.readNextSLEB128();
			}
		}
		
		System.out.printf("APS2: read reloc [0x%016x, 0x%016x, 0x%016x]\n", r_offset, r_info, r_addend);
		
		elfRelocation.supplyEntryData(r_offset, r_info, r_addend);
		
		relocationIndex++;
		relocationGroupIndex++;
	}

	private boolean isRelocationGroupedByInfo() {
		return (groupFlags & 1) != 0;
	}
	
	private boolean isRelocationGroupedByOffsetDelta() {
		return (groupFlags & 2) != 0;
	}
	
	private boolean isRelocationGroupedByAddend() {
		return (groupFlags & 4) != 0;
	}

	private boolean isRelocationGroupHasAddend() {
		return (groupFlags & 8) != 0;
	}

	private void readGroupFields(FactoryBundledWithBinaryReader reader) throws IOException {
		groupSize = reader.readNextSLEB128();
		groupFlags = reader.readNextSLEB128();
		
		if(isRelocationGroupedByOffsetDelta()) {
			groupROffsetDelta = reader.readNextSLEB128();
		}
		
		if(isRelocationGroupedByInfo()) {
			r_info = reader.readNextSLEB128();
		}
		
		if(isRelocationGroupHasAddend() &&
				isRelocationGroupedByAddend()) {
			if(!hasAddend) {
				throw new RuntimeException("unexpected r_addend");
			}
			r_addend+= reader.readNextSLEB128();
		} else if(!isRelocationGroupHasAddend()) {
			r_addend = 0;
		}
		
		relocationGroupIndex = 0;
		
		/*
		System.out.printf("APS2: starting new group (begins at %d, size %d, flags 0x%x)\n", relocationIndex, groupSize, groupFlags);
		System.out.printf("  r_offset delta: 0x%x\n", groupROffsetDelta);
		System.out.printf("  r_info: 0x%x\n", r_info);
		System.out.printf("  r_addend: 0x%x\n", r_addend);
		*/
	}

	@Override
	public void begin(FactoryBundledWithBinaryReader reader, long fileOffset, long tableSize) throws IOException {
		super.begin(reader, fileOffset, tableSize);
		byte[] header = reader.readNextByteArray(4);
		if(header[0] != 'A' || header[1] != 'P' || header[2] != 'S' || header[3] != '2') {
			throw new RuntimeException("invalid APS2 header");
		}
		totalCount = reader.readNextSLEB128();
		r_offset = reader.readNextSLEB128();
	}
	
	@Override
	public boolean shouldMarkup() {
		return false;
	}
}
