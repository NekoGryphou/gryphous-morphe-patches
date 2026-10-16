package app.morphe.patches.crunchyroll.pip

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

/**
 * The framework lifecycle name and posted watch-screen work are stable in
 * 3.112.2 and 3.125.0. The layout accessor and its return type are obfuscated
 * differently in each version, so do not use them to identify this hook.
 */
internal object WatchScreenOnUserLeaveHintFingerprint : Fingerprint(
    definingClass = "Lcom/crunchyroll/watchscreen/screen/WatchScreenActivity;",
    name = "onUserLeaveHint",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        methodCall(
            name = "onUserLeaveHint",
            parameters = emptyList(),
            returnType = "V",
            opcode = Opcode.INVOKE_SUPER,
        ),
        methodCall(smali = "Landroid/view/View;->post(Ljava/lang/Runnable;)Z"),
    ),
)
