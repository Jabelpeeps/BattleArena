package mc.alk.arena.objects.messaging;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mc.alk.arena.objects.messaging.MessageOptions.MessageOption;

@AllArgsConstructor
public class Message {
    @Getter final private String message;
    final private MessageOptions options;
    
    public Set<MessageOption> getOptions() {
        return options.getOptions();
    }
}
