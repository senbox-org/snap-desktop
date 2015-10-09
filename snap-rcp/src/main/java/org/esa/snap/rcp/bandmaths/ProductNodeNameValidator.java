package org.esa.snap.rcp.bandmaths;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.openide.util.NbBundle;

/**
 * Created by Norman on 30.06.2015.
 */
@NbBundle.Messages({
        "CTL_PNNV_ExMsg_UniqueBandName=The band name must be unique within the product scope.\n"
                + "The scope comprises bands and tie-point grids.",
        "CTL_PNNV_ExMsg_ContainedCharacter=The band name ''{0}'' is not valid.\n\n"
                + "Names must not start with a dot and must not\n"
                + "contain any of the following characters: \\/:*?\"<>|"

})
class ProductNodeNameValidator implements Validator {
    Product targetProduct;

    public ProductNodeNameValidator(Product targetProduct) {
        this.targetProduct = targetProduct;
    }

    @Override
    public void validateValue(Property property, Object value) throws ValidationException {
        final String name = (String) value;
        if (!ProductNode.isValidNodeName(name)) {
            throw new ValidationException(Bundle.CTL_PNNV_ExMsg_ContainedCharacter(name));
        }
        if (targetProduct.containsRasterDataNode(name)) {
            throw new ValidationException(Bundle.CTL_PNNV_ExMsg_UniqueBandName());
        }
    }
}
