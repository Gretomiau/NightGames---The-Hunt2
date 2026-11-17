import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.FieldInsnNode;
import jdk.internal.org.objectweb.asm.tree.JumpInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.VarInsnNode;

import java.nio.file.Files;
import java.nio.file.Path;

public final class PatchDynamicBufferManager {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Se requieren archivo de entrada y salida");
        }
        ClassReader reader = new ClassReader(Files.readAllBytes(Path.of(args[0])));
        ClassNode classNode = new ClassNode(Opcodes.ASM8);
        reader.accept(classNode, 0);
        MethodNode method = classNode.methods.stream()
            .filter(node -> node.name.equals("endFrame") && node.desc.equals("()V"))
            .findFirst()
            .orElseThrow();
        MethodInsnNode lastHasNext = null;
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode invoke
                && invoke.owner.equals("java/util/Iterator")
                && invoke.name.equals("hasNext")
                && invoke.desc.equals("()Z")) {
                lastHasNext = invoke;
            }
        }
        if (lastHasNext == null
            || !(lastHasNext.getPrevious() instanceof VarInsnNode load)
            || load.getOpcode() != Opcodes.ALOAD
            || load.var != 4
            || !(lastHasNext.getNext() instanceof JumpInsnNode jump)
            || jump.getOpcode() != Opcodes.IFNE) {
            throw new IllegalStateException("La versión de Veil no coincide con la esperada");
        }
        method.instructions.set(lastHasNext.getPrevious(), new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.insertBefore(lastHasNext,
            new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "swapShaders", "Ljava/util/Set;"));
        method.instructions.set(lastHasNext,
            new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Set", "isEmpty", "()Z", true));
        jump.setOpcode(Opcodes.IFEQ);
        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        Files.write(Path.of(args[1]), writer.toByteArray());
    }
}
