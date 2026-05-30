package br.hubpedro.infra.api.router;

import br.hubpedro.contracts.Router;
import br.hubpedro.contracts.annotations.Get;
import br.hubpedro.contracts.annotations.Post;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class ControllerScanner {

    @FunctionalInterface
    interface RouteAction {
        void register(Method method, Router router, Object controller);
    }

    private static final Map<Predicate<Method>, RouteAction> rules = new LinkedHashMap<>();

    static {
        rules.put(
            m -> m.isAnnotationPresent(Get.class),
            (method, router, controller) -> {
                Get getAnn = method.getAnnotation(Get.class);
                String path = getAnn.value();
                router.addRoute("GET", path, new ReflectionMethodHandler(controller, method));
                System.out.println("Controlador registrado [GET]: " + path + " -> " + controller.getClass().getSimpleName() + "." + method.getName());
            }
        );

        rules.put(
            m -> m.isAnnotationPresent(Post.class),
            (method, router, controller) -> {
                Post postAnn = method.getAnnotation(Post.class);
                String path = postAnn.value();
                router.addRoute("POST", path, new ReflectionMethodHandler(controller, method));
                System.out.println("Controlador registrado [POST]: " + path + " -> " + controller.getClass().getSimpleName() + "." + method.getName());
            }
        );
    }

    private ControllerScanner() {
    }

    public static void scanAndRegister(Router router, Object controller) {
        if (controller == null) {
            throw new IllegalArgumentException("O controlador não pode ser nulo.");
        }

        Method[] methods = controller.getClass().getDeclaredMethods();

        for (Method method : methods) {
            for (Map.Entry<Predicate<Method>, RouteAction> entry : rules.entrySet()) {
                if (entry.getKey().test(method)) {
                    entry.getValue().register(method, router, controller);
                }
            }
        }
    }
}
