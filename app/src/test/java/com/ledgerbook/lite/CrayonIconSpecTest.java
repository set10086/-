package com.ledgerbook.lite;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class CrayonIconSpecTest {
    @Test
    public void everyBuiltInCategoryResolvesToDrawableCrayonSpec() {
        Set<String> families = new HashSet<>();
        for (String type : new String[]{LedgerDb.TYPE_EXPENSE, LedgerDb.TYPE_INCOME,
                LedgerDb.TYPE_TRANSFER}) {
            for (InputCatalog.Option option : InputCatalog.categories(type)) {
                if (option.custom) continue;
                CrayonIconSpec spec = CrayonIconSpec.forCategory(type, option.label);
                assertNotNull(option.label, spec);
                assertFalse(option.label, spec.family.isEmpty());
                assertFalse(option.label, spec.mark.isEmpty());
                families.add(spec.family);
            }
        }
        assertTrue("Icon renderer must contain varied semantic families", families.size() >= 12);
    }

    @Test
    public void commonReferenceCategoriesUseExpectedSemanticFamilies() {
        assertTrue(CrayonIconSpec.forCategory(LedgerDb.TYPE_EXPENSE, "餐饮/早餐")
                .family.contains("food"));
        assertTrue(CrayonIconSpec.forCategory(LedgerDb.TYPE_EXPENSE, "交通/公交")
                .family.contains("transport"));
        assertTrue(CrayonIconSpec.forCategory(LedgerDb.TYPE_EXPENSE, "医疗/药品")
                .family.contains("medical"));
        assertTrue(CrayonIconSpec.forCategory(LedgerDb.TYPE_INCOME, "工资薪酬/工资")
                .family.contains("income"));
    }
}
