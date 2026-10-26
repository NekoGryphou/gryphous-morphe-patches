package app.morphe.patches.smartlife.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction

// Deliberately unnamed: automatically applied once through dependencies, never selectable.
internal val smartLifeSettingsPatch = bytecodePatch {
    compatibleWith(COMPATIBILITY_SMART_LIFE)
    extendWith("extensions/smartlife.mpe")

    execute {
        val application = Fingerprint(
            definingClass = "Lcom/smart/app/SmartApplication;",
            name = "onCreate", parameters = emptyList(), returnType = "V",
            filters = listOf(methodCall(name = "onCreate")),
        ).method
        application.addInstructions(1, "invoke-static/range {p0 .. p0}, $HOOKS->initialize(Landroid/app/Application;)V")

        val settings = Fingerprint(
            definingClass = "Lcom/thingclips/smart/personal/setting/plug/activity/SettingActivity;",
            name = "initData", parameters = emptyList(), returnType = "V",
            strings = listOf("personal_setting_config_list.json"),
        ).method
        val constructor = settings.implementation!!.instructions.indexOfFirst {
            (it as? ReferenceInstruction)?.reference?.toString() ==
                "Lcom/thingclips/smart/personal/archer/ArcherContainer;-><init>(Landroid/content/Context;Landroidx/lifecycle/LifecycleOwner;Ljava/util/List;Ljava/util/List;Ljava/util/List;)V"
        }
        val arguments = settings.implementation!!.instructions.getOrNull(constructor) as? RegisterRangeInstruction
            ?: throw PatchException("Unsupported Smart Life Settings constructor invocation")
        if (arguments.registerCount != 6) throw PatchException("Unexpected ArcherContainer arguments")
        val groups = arguments.startRegister + 3
        val cells = arguments.startRegister + 4
        // Both remote configuration and asset fallback converge here. Derive the two
        // List argument registers from the verified constructor, preserving cell-show overrides.
        settings.addInstructions(
            constructor,
            """
                invoke-static/range {v$groups .. v$groups}, ${EXTENSION}SettingsIntegration;->groups(Ljava/util/List;)Ljava/util/List;
                move-result-object v$groups
                invoke-static/range {v$cells .. v$cells}, ${EXTENSION}SettingsIntegration;->cells(Ljava/util/List;)Ljava/util/List;
                move-result-object v$cells
            """,
        )
    }
}
