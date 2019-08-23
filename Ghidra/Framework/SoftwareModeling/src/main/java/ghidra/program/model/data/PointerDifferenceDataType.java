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
package ghidra.program.model.data;

/**
 * Basic implementation for a Signed Long Integer dataType 
 */
public class PointerDifferenceDataType extends AbstractIntegerDataType {

	private final static long serialVersionUID = 1;

	/** A statically defined UnsignedLongDataType instance.*/
	public final static PointerDifferenceDataType dataType = new PointerDifferenceDataType();

	public PointerDifferenceDataType() {
		this(null);
	}

	public PointerDifferenceDataType(DataTypeManager dtm) {
		super("ptrdiff_t", true, dtm);
	}

	/**
	 * 
	 * @see ghidra.program.model.data.DataType#getLength()
	 */
	@Override
	public int getLength() {
		return getDataOrganization().getPointerSize();
	}

	/**
	 * @see ghidra.program.model.data.DataType#isDynamicallySized()
	 */
	@Override
	public boolean isDynamicallySized() {
		return true;
	}

	/**
	 * 
	 * @see ghidra.program.model.data.DataType#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Signed Pointer-Sized Integer (compiler-specific size)";
	}

	@Override
	public String getCDeclaration() {
		return C_POINTER_DIFFERENCE_T;
	}

	@Override
	public PointerSizeDataType getOppositeSignednessDataType() {
		return PointerSizeDataType.dataType.clone(getDataTypeManager());
	}

	@Override
	public PointerDifferenceDataType clone(DataTypeManager dtm) {
		if (dtm == getDataTypeManager()) {
			return this;
		}
		return new PointerDifferenceDataType(dtm);
	}

	@Override
	public String getCTypeDeclaration(DataOrganization dataOrganization) {
		return getCTypeDeclaration(getName(), "ptrdiff_t", false); // standard C-primitive type with modified name
	}

}
