package app.morphe.patches.smartlife.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

internal fun BytecodePatchContext.enableSetting(name: String) {
    val method = mutableClassDefBy("${EXTENSION}Availability;").methods.single { it.name == name }
    val instructions = method.implementation!!.instructions
    if (instructions.size != 2 || instructions[0].opcode != Opcode.CONST_4 ||
        instructions[1].opcode != Opcode.RETURN
    ) throw PatchException("Unexpected Smart Life availability method: $name")
    val register = (instructions[0] as OneRegisterInstruction).registerA
    method.replaceInstruction(0, "const/4 v$register, 0x1")
}

internal fun MutableClass.addMethod(
    name: String,
    parameters: List<String>,
    returnType: String,
    registers: Int,
    code: String,
    access: Int = AccessFlags.PUBLIC.value,
) {
    if (methods.any { it.name == name && it.parameterTypes == parameters }) {
        throw PatchException("Smart Life hook already exists: $type->$name")
    }
    val method = MutableMethod(
        ImmutableMethod(
            type, name, parameters.map { ImmutableMethodParameter(it, emptySet(), null) },
            returnType, access, emptySet(), emptySet(), MutableMethodImplementation(registers),
        ),
    )
    method.addInstructions(0, code)
    methods.add(method)
}

/** Transform every returned object without introducing scratch registers or skipping native code. */
internal fun MutableMethod.mapReturns(hook: String, type: String) {
    val implementation = implementation ?: throw PatchException("Missing body for $this")
    implementation.instructions.indices.reversed().forEach { index ->
        val instruction = implementation.instructions[index]
        if (instruction.opcode != Opcode.RETURN_OBJECT) return@forEach
        val register = (instruction as OneRegisterInstruction).registerA
        addInstructions(
            index,
            """
                invoke-static/range {v$register .. v$register}, $HOOKS->$hook($type)$type
                move-result-object v$register
            """,
        )
    }
}

internal fun MutableMethod.hideCellWhen(hook: String) {
    if ((implementation?.registerCount ?: 0) <= parameters.size + 1) {
        throw PatchException("No local register for Smart Life cell visibility: $this")
    }
    addInstructions(
        0,
        """
            invoke-static {}, $HOOKS->$hook()Z
            move-result v0
            if-eqz v0, :morphe_original_cell
            const/4 v0, 0x0
            invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
            move-result-object v0
            return-object v0
            :morphe_original_cell
            nop
        """,
    )
}
