package app.morphe.patches.smartlife

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.smartlife.shared.COMPATIBILITY_SMART_LIFE
import app.morphe.patches.smartlife.shared.HOOKS
import app.morphe.patches.smartlife.shared.addMethod
import app.morphe.patches.smartlife.shared.enableSetting
import app.morphe.patches.smartlife.shared.hideCellWhen
import app.morphe.patches.smartlife.shared.mapReturns
import app.morphe.patches.smartlife.shared.smartLifeSettingsPatch
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val hideHomePlusPatch = bytecodePatch(
    name = "Hide Home Plus",
    description = "Adds a setting to hide the Home header Plus badge while keeping the separate add (+) button.",
) {
    compatibleWith(COMPATIBILITY_SMART_LIFE)
    dependsOn(smartLifeSettingsPatch)
    execute {
        val menu = Fingerprint(
            returnType = "Landroid/view/View;",
            parameters = listOf("Landroid/content/Context;", "Landroidx/lifecycle/LifecycleOwner;", "Landroidx/appcompat/widget/Toolbar;"),
            filters = listOf(methodCall(smali = "Lcom/thingclips/smart/ai/assistant/api/AIAssistantEnableService;->L2(Landroid/content/Context;)Landroid/view/View;")),
            custom = { _, clazz -> clazz.sourceFile == "AIPlusMenuItem.kt" },
        ).method
        val clazz = mutableClassDefBy(menu.definingClass)
        // Native HomeMenuService checks isEnabled before inflating/allocating toolbar space.
        clazz.addMethod("isEnabled", emptyList(), "Z", 2, """
            invoke-static {}, $HOOKS->hideHomePlus()Z
            move-result v0
            xor-int/lit8 v0, v0, 0x1
            return v0
        """)
        enableSetting("homePlus")
    }
}

@Suppress("unused")
val hideHomeRecommendationsPatch = bytecodePatch(
    name = "Hide Home Recommendations",
    description = "Adds a setting to remove the Home Recommended card group, including its header and spacing.",
) {
    compatibleWith(COMPATIBILITY_SMART_LIFE)
    dependsOn(smartLifeSettingsPatch)
    execute {
        // AsyncSimpleList is a presentation list. The runtime filters only HomeCardData's
        // verified DATA_TYPE_FIND_MORE (104); other lists and the backing model are intact.
        val submit = Fingerprint(
            returnType = "V", parameters = listOf("Ljava/util/List;"),
            filters = listOf(methodCall(smali = "Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z")),
            custom = { _, clazz -> clazz.sourceFile == "AsyncSimpleList.kt" },
        ).method
        submit.addInstructions(0, """
            invoke-static/range {p1 .. p1}, $HOOKS->homeCards(Ljava/util/List;)Ljava/util/List;
            move-result-object p1
        """)
        enableSetting("homeRecommendations")
    }
}

@Suppress("unused")
val hideAiInputBarPatch = bytecodePatch(
    name = "Hide AI Input Bar",
    description = "Adds a setting to hide the shared AI input bar and its floating/Home containers throughout the app.",
) {
    compatibleWith(COMPATIBILITY_SMART_LIFE)
    dependsOn(smartLifeSettingsPatch)
    execute {
        val components = listOf(
            "Lcom/thingclips/smart/ai/assistant/view/AIBarView;",
            "Lcom/thingclips/smart/ai/aiassistant/ui/view/AIHomeEntryView;",
            "Lcom/thingclips/smart/ai/aiassistant/ui/view/InnerFloatAIBarLayout;",
        )
        for (type in components) {
            val clazz = mutableClassDefBy(type)
            clazz.addMethod("setVisibility", listOf("I"), "V", 2, """
                invoke-static {p0, p1}, $HOOKS->aiVisibility(Landroid/view/View;I)I
                move-result p1
                invoke-super {p0, p1}, ${clazz.superclass}->setVisibility(I)V
                return-void
            """)
            // At the end of real constructors, after superclass and component initialization.
            clazz.methods.filter { it.name == "<init>" && it.parameterTypes.none { p -> p == "Lkotlin/jvm/internal/DefaultConstructorMarker;" } }
                .forEach { method ->
                    method.implementation!!.instructions.indices.reversed().forEach { index ->
                        if (method.implementation!!.instructions[index].opcode == Opcode.RETURN_VOID) {
                            method.addInstructions(index, "invoke-static/range {p0 .. p0}, $HOOKS->hideAiView(Landroid/view/View;)V")
                        }
                    }
                }
        }
        enableSetting("aiInputBar")
    }
}

@Suppress("unused")
val hideIntelligenceRecommendationsPatch = bytecodePatch(
    name = "Hide Intelligence Recommendations",
    description = "Adds an independent setting to hide the Intelligence Recommended page.",
) {
    compatibleWith(COMPATIBILITY_SMART_LIFE)
    dependsOn(smartLifeSettingsPatch)
    execute {
        val compose = Fingerprint(
            name = "b", returnType = "Ljava/util/Map;", parameters = listOf("Ljava/util/Map;"),
            strings = listOf("sceneSceneTab", "DefaultSceneTabImpl"),
            custom = { _, clazz -> clazz.sourceFile == "DefaultSceneTabImpl.kt" },
        ).method
        compose.mapReturns("sceneTabs", "Ljava/util/Map;")
        val pages = Fingerprint(
            definingClass = "Lcom/thingclips/smart/scene/home/tab/c;",
            returnType = "Ljava/util/List;", parameters = emptyList(),
            strings = listOf("tab compose: ngBase="),
        ).method
        pages.mapReturns("sceneTabIds", "Ljava/util/List;")
        val clazz = mutableClassDefBy(pages.definingClass)
        clazz.addMethod("saveState", emptyList(), "Landroid/os/Parcelable;", 2, """
            invoke-super {p0}, ${clazz.superclass}->saveState()Landroid/os/Parcelable;
            move-result-object v0
            invoke-static {v0}, $HOOKS->saveSceneState(Landroid/os/Parcelable;)Landroid/os/Parcelable;
            move-result-object v0
            return-object v0
        """)
        clazz.addMethod("restoreState", listOf("Landroid/os/Parcelable;", "Ljava/lang/ClassLoader;"), "V", 3, """
            invoke-static {p1}, $HOOKS->restoreSceneState(Landroid/os/Parcelable;)Landroid/os/Parcelable;
            move-result-object p1
            invoke-super {p0, p1, p2}, ${clazz.superclass}->restoreState(Landroid/os/Parcelable;Ljava/lang/ClassLoader;)V
            return-void
        """)
        enableSetting("intelligenceRecommendations")
    }
}

@Suppress("unused")
val hideExplorerTabPatch = bytecodePatch(
    name = "Hide Explorer Tab",
    description = "Adds a setting to remove Explorer from navigation, with safe selection and fragment restoration.",
) {
    compatibleWith(COMPATIBILITY_SMART_LIFE)
    dependsOn(smartLifeSettingsPatch)
    execute {
        val build = Fingerprint(
            definingClass = "Lmv0;", name = "b", returnType = "V", parameters = listOf("Landroid/content/Context;"),
            strings = listOf("single tab", "invalid tab config"),
        ).method
        build.addInstructions(0, """
            iget-object v0, p0, Lmv0;->b:Ljava/util/List;
            invoke-static {v0}, $HOOKS->navigationTabs(Ljava/util/List;)Ljava/util/List;
            move-result-object v0
            iput-object v0, p0, Lmv0;->b:Ljava/util/List;
            iget-object v0, p0, Lmv0;->a:Ljava/lang/String;
            invoke-static {v0}, $HOOKS->navigationDestination(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v0
            iput-object v0, p0, Lmv0;->a:Ljava/lang/String;
        """)
        // Handles intent/deep-link and remembered selection, before the app resolves an index.
        val select = Fingerprint(
            definingClass = "Lov0;", name = "w", returnType = "I", parameters = listOf("Ljava/lang/String;"),
            filters = listOf(methodCall(smali = "Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z")),
        ).method
        select.addInstructions(0, """
            invoke-static/range {p1 .. p1}, $HOOKS->navigationDestination(Ljava/lang/String;)Ljava/lang/String;
            move-result-object p1
        """)
        val adapter = mutableClassDefBy("Lcom/thingclips/smart/appshell/adapter/AppShellAdapter;")
        adapter.addMethod("e", listOf("I"), "J", 4, """
            invoke-static {p1}, $HOOKS->navigationItemId(I)J
            move-result-wide v0
            return-wide v0
        """)
        enableSetting("explorerTab")
    }
}

@Suppress("unused")
val hideTuyaAiWebVersionPatch = bytecodePatch(
    name = "Hide Tuya AI Web Version",
    description = "Adds a setting to hide only the Tuya AI Web Version entry in Profile.",
) {
    compatibleWith(COMPATIBILITY_SMART_LIFE)
    dependsOn(smartLifeSettingsPatch)
    execute {
        Fingerprint(
            definingClass = "Lcom/thingclips/smart/personal/center/plug/cell/WebAppCell;",
            name = "e", returnType = "Ljava/lang/Object;", parameters = listOf("Lkotlin/coroutines/Continuation;"),
            strings = listOf("is_show_webapp"),
        ).method.hideCellWhen("hideTuyaWeb")
        enableSetting("tuyaWeb")
    }
}

@Suppress("unused")
val hideBenefitsPatch = bytecodePatch(
    name = "Hide Centre d’avantages",
    description = "Adds an independent setting to hide the Centre d’avantages (Rewards Hub) Profile entry.",
) {
    compatibleWith(COMPATIBILITY_SMART_LIFE)
    dependsOn(smartLifeSettingsPatch)
    execute {
        Fingerprint(
            definingClass = "Lcom/thingclips/smart/personal/center/plug/cell/WelfareEntryCell;",
            name = "e", returnType = "Ljava/lang/Object;", parameters = listOf("Lkotlin/coroutines/Continuation;"),
            strings = listOf("me_entry_list_row"),
        ).method.hideCellWhen("hideBenefits")
        enableSetting("benefits")
    }
}
