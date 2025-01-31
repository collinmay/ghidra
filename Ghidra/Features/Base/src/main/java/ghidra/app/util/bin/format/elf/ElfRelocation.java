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

import generic.continues.GenericFactory;
import ghidra.app.util.bin.ByteArrayConverter;
import ghidra.app.util.bin.StructConverter;
import ghidra.app.util.bin.format.FactoryBundledWithBinaryReader;
import ghidra.app.util.bin.format.elf.extend.ElfLoadAdapter;
import ghidra.program.model.data.*;
import ghidra.util.Conv;
import ghidra.util.DataConverter;
import ghidra.util.exception.AssertException;

/**
 * A class to represent the Elf32_Rel and Elf64_Rel data structure.
 * <br>
 * <pre>
 * typedef uint32_t Elf32_Addr;
 * typedef uint64_t Elf64_Addr;
 * typedef uint32_t Elf32_Word;
 * typedef uint64_t Elf64_Xword;
 * 
 * REL entry:
 * 
 * typedef struct {
 *     Elf32_Addr   r_offset;
 *     Elf32_Word   r_info;
 * } Elf32_Rel;
 * 
 * typedef struct {
 *     Elf64_Addr   r_offset;
 *     Elf64_Xword  r_info;
 * } Elf64_Rel;
 * 
 * RELA entry with addend:
 * 
 *  * typedef struct {
 *     Elf32_Addr    r_offset;
 *     Elf32_Word    r_info;
 *     Elf32_Sword   r_addend;
 * } Elf32_Rela;
 * 
 * typedef struct {
 *     Elf64_Addr    r_offset;   //Address
 *     Elf64_Xword   r_info;     //Relocation type and symbol index
 *     Elf64_Sxword  r_addend;   //Addend 
 * } Elf64_Rela;
 *
 * </pre>
 */
public class ElfRelocation implements ByteArrayConverter, StructConverter {

	protected static final String R_OFFSET_COMMENT = "location to apply the relocation action";
	protected static final String R_INFO_COMMENT =
		"the symbol table index and the type of relocation";
	protected static final String R_ADDEND_COMMENT =
		"a constant addend used to compute the relocatable field value";

	private long r_offset;
	private long r_info;
	private long r_addend;

	private boolean hasAddend;
	private ElfHeader elfHeader;
	private int relocationIndex;

	/**
	 * GenericFactory construction and initialization method for a ELF representative 
	 * relocation entry (entry data will be 0)
	 * @param factory instantiation factory.
	 * @param elfHeader ELF header
	 * @param relocationIndex index of entry in relocation table
	 * @param withAddend true if if RELA entry with addend, else false
	 * @return ELF relocation object
	 */
	static ElfRelocation createElfRelocation(GenericFactory factory, ElfHeader elfHeader,
			int relocationIndex, boolean withAddend) {

		Class<? extends ElfRelocation> elfRelocationClass = getElfRelocationClass(elfHeader);
		ElfRelocation elfRelocation = (ElfRelocation) factory.create(elfRelocationClass);
		try {
			elfRelocation.initElfRelocation(elfHeader, relocationIndex, withAddend);
		}
		catch (IOException e) {
			// absence of reader should prevent any IOException from occurring
			throw new AssertException("unexpected IO error", e);
		}
		return elfRelocation;
	}

	static Class<? extends ElfRelocation> getElfRelocationClass(ElfHeader elfHeader) {
		Class<? extends ElfRelocation> elfRelocationClass = null;
		ElfLoadAdapter loadAdapter = elfHeader.getLoadAdapter();
		if (loadAdapter != null) {
			elfRelocationClass = loadAdapter.getRelocationClass(elfHeader);
		}
		if (elfRelocationClass == null) {
			elfRelocationClass = ElfRelocation.class;
		}
		return elfRelocationClass;
	}

	/**
	 * DO NOT USE THIS CONSTRUCTOR, USE create*(GenericFactory ...) FACTORY METHODS INSTEAD.
	 * @see ElfRelocation#createElfRelocation
	 */
	public ElfRelocation() {
	}

	/**
	 * Initialize ELF relocation entry. 
	 * @param elfHeader ELF header
	 * @param relocationTableIndex index of relocation within relocation table
	 * @param withAddend true if if RELA entry with addend, else false
	 * @throws IOException
	 */
	protected void initElfRelocation(ElfHeader elfHeader,
			int relocationTableIndex, boolean withAddend) throws IOException {
		this.elfHeader = elfHeader;
		this.relocationIndex = relocationTableIndex;
		this.hasAddend = withAddend;
	}
	
	public void supplyEntryData(long r_offset, long r_info, long r_addend) {
		this.r_offset = r_offset;
		this.r_info = r_info;
		this.r_addend = r_addend;
	}

	/**
	 * @return index of relocation within its corresponding relocation table
	 */
	public int getRelocationIndex() {
		return relocationIndex;
	}

	/**
	 * @return true if processing a 32-bit header, else 64-bit
	 */
	protected boolean is32Bit() {
		return elfHeader.is32Bit();
	}
	
	/**
	 * @return ELF header
	 */
	protected ElfHeader getElfHeader() {
		return elfHeader;
	}

	/**
	 * This member gives the location at which to apply the relocation action. 
	 * 
	 * For a relocatable file, the value is the byte offset from the 
	 * beginning of the section to the storage unit affected by the relocation. 
	 * 
	 * For an executable file or a shared object, the value is the virtual address of
	 * the storage unit affected by the relocation.
	 * 
	 * @return the location at which to apply the relocation
	 */
	public long getOffset() {
		return r_offset;
	}

	/**
	 * Sets the relocation offset to the new specified value.
	 * @param offset the new offset value
	 */
	public void setOffset(int offset) {
		this.r_offset = offset & Conv.INT_MASK;
	}

	/**
	 * Sets the relocation offset to the new specified value.
	 * @param offset the new offset value
	 */
	public void setOffset(long offset) {
		this.r_offset = offset;
	}

	/**
	 * Returns the symbol index where the relocation must be made.
	 * @return the symbol index
	 */
	public int getSymbolIndex() {
		return (int) (is32Bit() ? (r_info >> 8) : (r_info >> 32));
	}

	/**
	 * The type of relocation to apply.
	 * NOTE: Relocation types are processor-specific.
	 * @return type of relocation to apply
	 */
	public int getType() {
		return (int) (is32Bit() ? (r_info & Conv.BYTE_MASK) : (r_info & Conv.INT_MASK));
	}

	/**
	 * Returns the r_info relocation entry field value
	 * @return r_info value
	 */
	public long getRelocationInfo() {
		return r_info;
	}

	/**
	 * This member specifies a constant addend used to compute 
	 * the value to be stored into the relocatable field.  This
	 * value will be 0 for REL entries which do not supply an addend.
	 * @return a constant addend
	 */
	public long getAddend() {
		return r_addend;
	}

	/**
	 * Returns true if this is a RELA entry with addend
	 * @return true if this is a RELA entry with addend
	 */
	public boolean hasAddend() {
		return hasAddend;
	}

	@Override
	public DataType toDataType() {
		String dtName = is32Bit() ? "Elf32_Rel" : "Elf64_Rel";
		if (hasAddend) {
			dtName += "a";
		}
		Structure struct = new StructureDataType(new CategoryPath("/ELF"), dtName, 0);
		DataType fieldDt = is32Bit() ? DWORD : QWORD;
		struct.add(fieldDt, "r_offset", R_OFFSET_COMMENT);
		struct.add(fieldDt, "r_info", R_INFO_COMMENT);
		if (hasAddend) {
			struct.add(fieldDt, "r_addend", R_ADDEND_COMMENT);
		}
		return struct;
	}

	/**
	 * @see ghidra.app.util.bin.ByteArrayConverter#toBytes(ghidra.util.DataConverter)
	 */
	@Override
	public byte[] toBytes(DataConverter dc) {
		byte[] bytes = new byte[sizeof()];
		if (is32Bit()) {
			dc.putInt(bytes, 0, (int) r_offset);
			dc.putInt(bytes, 4, (int) r_info);
		}
		else {
			dc.putLong(bytes, 0, r_offset);
			dc.putLong(bytes, 8, r_info);
		}
		return bytes;
	}

	@Override
	public String toString() {
		String str = "Offset: 0x" + Long.toHexString(getOffset()) + " - Type: 0x" +
			Long.toHexString(getType()) + " - Symbol: 0x" + Long.toHexString(getSymbolIndex());
		if (hasAddend) {
			str += " - Addend: 0x" + Long.toHexString(getAddend());
		}
		return str;
	}

	protected int sizeof() {
		if (hasAddend) {
			return is32Bit() ? 12 : 24;
		}
		return is32Bit() ? 8 : 16;
	}
}
