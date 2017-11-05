package org.jsonnet.vm;

public class Vm {
    /**
     * Execute the program and return the value as a JSON string.
     *
     * @param alloc The allocator used to create the ast.
     * @param ast The program to execute.
     * @param ext The external vars / code.
     * @param max_stack Recursion beyond this level gives an error.
     * @param gc_min_objects The garbage collector does not run when the heap is this small.
     * @param gc_growth_trigger Growth since last garbage collection cycle to trigger a new cycle.
     * @param import_callback A callback to handle imports
     * @param import_callback_ctx Context param for the import callback.
     * @param output_string Whether to expect a string and output it without JSON encoding
     * @throws RuntimeError reports runtime errors in the program.
     * @returns The JSON result in string form.
     */
    /*
    public static String jsonnet_vm_execute(Allocator *alloc,
                                            const AST *ast,
                                            const std::map<std::string, VmExt> &ext,
                                            unsigned max_stack,
                                            double gc_min_objects,
                                            double gc_growth_trigger,
                                            const VmNativeCallbackMap &natives,
                                            JsonnetImportCallback *import_callback,
                                            void *import_callback_ctx,
                                            bool string_output) {
        final Interpreter vm = new Interpreter(alloc,
                                               ext_vars,
                                               max_stack,
                                               gc_min_objects,
                                               gc_growth_trigger,
                                               natives,
                                               import_callback,
                                               ctx);
        vm.evaluate(ast, 0);
        if (string_output) {
            return encode_utf8(vm.manifestString(LocationRange("During manifestation")));
        } else {
            return encode_utf8(vm.manifestJson(LocationRange("During manifestation"), true, U""));
        }
    }
    */

    /**
     * Execute the program and return the value as a number of named JSON files.
     *
     * This assumes the given program yields an object whose keys are filenames.
     *
     * @param alloc The allocator used to create the ast.
     * @param ast The program to execute.
     * @param ext The external vars / code.
     * @param tla The top-level arguments (strings or code).
     * @param max_stack Recursion beyond this level gives an error.
     * @param gc_min_objects The garbage collector does not run when the heap is this small.
     * @param gc_growth_trigger Growth since last garbage collection cycle to trigger a new cycle.
     * @param import_callback A callback to handle imports
     * @param import_callback_ctx Context param for the import callback.
     * @param output_string Whether to expect a string and output it without JSON encoding
     * @throws RuntimeError reports runtime errors in the program.
     * @returns A mapping from filename to the JSON strings for that file.
     */
    /*
    public static Map<String, String> jsonnet_vm_execute_multi(Allocator *alloc,
                                                               const AST *ast,
                                                               const std::map<std::string, VmExt> &ext,
                                                               unsigned max_stack,
                                                               double gc_min_objects,
                                                               double gc_growth_trigger,
                                                               const VmNativeCallbackMap &natives,
                                                               JsonnetImportCallback *import_callback,
                                                               void *import_callback_ctx,
                                                               bool string_output) {
        final Interpreter vm = new Interpreter(alloc,
                                               ext_vars,
                                               max_stack,
                                               gc_min_objects,
                                               gc_growth_trigger,
                                               natives,
                                               import_callback,
                                               ctx);
        vm.evaluate(ast, 0);
        return vm.manifestMulti(string_output);
    }
    */

    /**
     * Execute the program and return the value as a stream of JSON files.
     *
     * This assumes the given program yields an array whose elements are individual
     * JSON files.
     *
     * @param alloc The allocator used to create the ast.
     * @param ast The program to execute.
     * @param ext The external vars / code.
     * @param tla The top-level arguments (strings or code).
     * @param max_stack Recursion beyond this level gives an error.
     * @param gc_min_objects The garbage collector does not run when the heap is this small.
     * @param gc_growth_trigger Growth since last garbage collection cycle to trigger a new cycle.
     * @param import_callback A callback to handle imports
     * @param import_callback_ctx Context param for the import callback.
     * @param output_string Whether to expect a string and output it without JSON encoding
     * @throws RuntimeError reports runtime errors in the program.
     * @returns A mapping from filename to the JSON strings for that file.
     */
    /*
    public static List<String> jsonnet_vm_execute_stream(Allocator *alloc,
                                                         const AST *ast,
                                                         const std::map<std::string, VmExt> &ext,
                                                         unsigned max_stack,
                                                         double gc_min_objects,
                                                         double gc_growth_trigger,
                                                         const VmNativeCallbackMap &natives,
                                                         JsonnetImportCallback *import_callback,
                                                         void *import_callback_ctx) {
        final Interpreter vm = new Interpreter(alloc,
                                               ext_vars,
                                               max_stack,
                                               gc_min_objects,
                                               gc_growth_trigger,
                                               natives,
                                               import_callback,
                                               ctx);
        vm.evaluate(ast, 0);
        return vm.manifestStream();
    }
    */

    /**
     * Turn a path e.g. "/a/b/c" into a dir, e.g. "/a/b/".  If there is no path returns "".
     */
    public static String dir_name(final String path) {
        final int last_slash = path.lastIndexOf('/');
        if (last_slash != -1) {
            return path.substring(0, last_slash + 1);
        }
        return "";
    }

    /*
    typedef const AST *(Interpreter::*BuiltinFunc)(const LocationRange &loc,
                                                   const std::vector<Value> &args);
    */
}
