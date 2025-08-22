package com.aref.cloud_assistant_mcp.views;

import com.aref.cloud_assistant_mcp.dto.ConversationCreateDto;
import com.aref.cloud_assistant_mcp.dto.ResponseType;
import com.aref.cloud_assistant_mcp.views.utils.MarkdownUtil;
import com.aref.cloud_assistant_mcp.views.vaadin.Conversation;
import com.aref.cloud_assistant_mcp.views.vaadin.Spinner;
import com.aref.cloud_assistant_mcp.views.vaadin.ViewChatService;
import com.aref.cloud_assistant_mcp.views.vaadin.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import dev.langchain4j.data.message.ChatMessageType;
import jakarta.annotation.security.PermitAll;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Route(value = "")
@PageTitle("AWS ChatBot")
@PermitAll
@Push
@CssImport("./styles/styles.css")
public class ChatView extends HorizontalLayout implements AppShellConfigurator {

    private static final Logger log = LoggerFactory.getLogger(ChatView.class);

    private static final String WIDTH_SIDEBAR = "250px";
    private static final String PLACEHOLDER_INPUT = "Type your message...";
    private static final String BTN_NEW_CHAT = "+ New Chat";
    private static final String TITLE_CHATS = "Chats";
    private static final String DEFAULT_CONV_TITLE = "New Conversation";
    private static final String CSS_SIDEBAR = "sidebar";
    private static final String CSS_H6 = "h6";
    private static final String CSS_BTN_SIDEBAR = "btn-sidebar";
    private static final String CSS_SCROLLER = "scroller";
    private static final String CSS_MSG_USER = "message-user";
    private static final String CSS_MSG_BOT = "message-bot";
    private static final String CSS_TOOLS_WRAPPER = "div-tools";
    private static final String CSS_TOOLS_TEXT = "div-tools-text";
    private static final String TOOL_RUNNING_PREFIX = "🛠 ";
    private static final String TOOL_RUNNING_SUFFIX = " is being executed.";

    private final ViewChatService chatService;

    private final VerticalLayout sidebar = new VerticalLayout();
    private final VerticalLayout chatBox = new VerticalLayout();
    private final Scroller messageScroller = new Scroller();
    private final VerticalLayout messageList = new VerticalLayout();
    private final TextField input = new TextField();
    private final Button send = new Button("Send");

    private Div currentBotMessageDiv;
    private NativeLabel currentBotMessageText;
    private Conversation currentConversation;

    private Disposable streamSubscription;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public ChatView(ViewChatService chatService) {
        this.chatService = Objects.requireNonNull(chatService, "chatService must not be null");
        this.chatService.clear();

        configureRootLayout();
        setupSidebar();
        setupChatBox();

        add(sidebar, chatBox);
        expand(chatBox);
    }

    private void configureRootLayout() {
        setSizeFull();
        setSpacing(false);
        setPadding(false);
    }

    private void setupSidebar() {
        sidebar.setWidth(WIDTH_SIDEBAR);
        sidebar.setPadding(true);
        sidebar.addClassName(CSS_SIDEBAR);

        Button newChatBtn = buildNewChatButton();
        sidebar.add(newChatBtn);

        H6 conversationsTitle = new H6(TITLE_CHATS);
        conversationsTitle.addClassName(CSS_H6);
        sidebar.add(conversationsTitle);

        chatService.getAllConversations().forEach(this::addConversationBtn);
    }

    private Button buildNewChatButton() {
        Button newChatBtn = new Button(BTN_NEW_CHAT);
        newChatBtn.setWidthFull();
        newChatBtn.addClickListener(e -> onNewConversation());
        return newChatBtn;
    }

    private void setupChatBox() {
        chatBox.setSizeFull();
        chatBox.setPadding(true);
        chatBox.setSpacing(true);
        chatBox.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        chatBox.setJustifyContentMode(JustifyContentMode.CENTER);
        chatBox.setAlignItems(Alignment.CENTER);

        messageScroller.setContent(messageList);
        messageScroller.setSizeFull();
        messageScroller.addClassName(CSS_SCROLLER);
        messageList.setWidthFull();

        chatBox.add(messageScroller);
        chatBox.add(buildInputLayout());
    }

    private HorizontalLayout buildInputLayout() {
        HorizontalLayout inputLayout = new HorizontalLayout(input, send);
        inputLayout.setWidthFull();

        input.setPlaceholder(PLACEHOLDER_INPUT);
        input.setWidthFull();
        input.addKeyDownListener(Key.ENTER, e -> sendMessage());

        send.addClickListener(e -> sendMessage());
        return inputLayout;
    }

    private void onNewConversation() {
        currentConversation = chatService.createNewConversation();
        messageList.removeAll();
        addConversationBtn(currentConversation);
    }

    public void addConversationBtn(Conversation conversation) {
        String title = conversation.getTitle() != null ? conversation.getTitle() : DEFAULT_CONV_TITLE;
        Button btn = new Button(title);
        btn.setWidthFull();
        btn.addClassName(CSS_BTN_SIDEBAR);
        btn.setId(conversation.getId());
        btn.addClickListener(e -> loadConversation(conversation));
        sidebar.add(btn);
    }

    public void changeSideBarTitle(String oldId, Conversation conversation) {
        sidebar.getChildren()
                .filter(component -> component instanceof Button)
                .filter(component -> component.getId().map(oldId::equals).orElse(false))
                .findAny()
                .ifPresent(component -> {
                    Button button = (Button) component;
                    button.setText(conversation.getTitle().replace("\"", ""));
                    button.setId(conversation.getId());
                });
    }

    private void loadConversation(Conversation conversation) {
        this.currentConversation = conversation;
        messageList.removeAll();
        chatService.getConversationMessages(conversation.getId()).forEach(this::displayMessage);
    }

    private void displayMessage(Message message) {
        Div bubble = new Div();
        if (message.getMessageType() == ChatMessageType.USER)
            bubble.addClassName(CSS_MSG_USER);
        else if (message.getMessageType() == ChatMessageType.AI)
            bubble.addClassName(CSS_MSG_BOT);
        else if (message.getMessageType() == ChatMessageType.TOOL_EXECUTION_RESULT)
            bubble.addClassName(CSS_TOOLS_WRAPPER);

        if (message.getMessageType() == ChatMessageType.TOOL_EXECUTION_RESULT) {
            showToolRunning(bubble, message.getToolName(), false);
            showToolResponse(bubble, message.getToolResponse());
        } else {
            String markdownText = message.getText();
            String html = MarkdownUtil.toHtml(markdownText);
            bubble.getElement().setProperty("innerHTML", html);
        }
        messageList.add(bubble);
        scrollToBottom();
    }

    private void sendMessage() {
        if (input.isEmpty() || currentConversation == null) return;

        final String text = input.getValue();
        input.clear();
        send.setDisableOnClick(true);

        Message userMsg = new Message(text, ChatMessageType.USER);
        chatService.addMessage(currentConversation.getId(), userMsg);
        displayMessage(userMsg);

        String conversationId = normalizeConversationId(currentConversation.getId());
        if (streamSubscription != null && !streamSubscription.isDisposed()) {
            streamSubscription.dispose();
        }
        streamSubscription = chatService.create(new ConversationCreateDto(conversationId, text, currentConversation.getTitle()))
                .doOnNext(this::handleServerEvent)
                .doOnError(err -> {
                    getUI().ifPresent(ui -> ui.access(() -> {
                        Notification n = Notification.show("Something went wrong. Please try again.");
                        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }));
                    log.error("Streaming error", err);
                })
                .doFinally(sig -> getUI().ifPresent(ui -> ui.access(() -> {
                    send.setDisableOnClick(false);
                })))
                .subscribe();
    }

    private String normalizeConversationId(String id) {
        return (id != null && !id.contains("temp")) ? id : null;
    }

    private void handleServerEvent(String serverPayload) {
        JSONObject json = new JSONObject(serverPayload);
        String type = json.getString("type");

        if (type.equals(ResponseType.CONVERSATION_DETAIL_METADATA.getValue())) {
            handleConversationMetadata(json);
        } else if (type.equals(ResponseType.MESSAGE.getValue())) {
            handleMessageAppend(json);
        } else if (type.equals(ResponseType.TOOL_CALLING.getValue())) {
            handleToolCalling(json);
        }
    }

    private void handleConversationMetadata(JSONObject json) {
        getUI().ifPresent(ui -> ui.access(() -> {
            String newConversationId = json.getString("conversationId");
            String title = json.getString("title");
            String oldId = currentConversation.getId();

            chatService.renameConversation(currentConversation.getId(), newConversationId, title);
            changeSideBarTitle(oldId, currentConversation);
            currentConversation.setId(newConversationId);

            Message botMsg = new Message(ChatMessageType.AI, ResponseType.MESSAGE.getValue());
            chatService.addMessage(newConversationId, botMsg);

            currentBotMessageDiv = new Div();
            currentBotMessageDiv.addClassName(CSS_MSG_BOT);

            currentBotMessageText = new NativeLabel("");
            currentBotMessageDiv.add(currentBotMessageText);

            messageList.add(currentBotMessageDiv);
            scrollToBottom();
        }));
    }

    private void handleMessageAppend(JSONObject json) {
        String messageChunk = json.getString("message");

        Conversation conversation = chatService.getConversation(currentConversation.getId());
        Message lastMessage = conversation.getLastMessage();
        lastMessage.appendText(messageChunk);

        getUI().ifPresent(ui -> ui.access(() -> {
            if (currentBotMessageText != null) {
                String markdownText = lastMessage.getText();
                String html = MarkdownUtil.toHtml(markdownText);
                currentBotMessageText.getElement().setProperty("innerHTML", html);
                ui.push();
                scrollToBottom();
            }
        }));
    }

    private void handleToolCalling(JSONObject json) {
        String toolName = json.getString("toolName");

        getUI().ifPresent(ui -> ui.access(() -> {
            if (currentBotMessageText == null) return;

            Div toolsWrapper = ensureToolsWrapper(currentBotMessageDiv);

            if (json.getBoolean("completed")) {
                hideSpinner(toolsWrapper);
                renderToolResponse(toolsWrapper, json);
            } else {
                showToolRunning(toolsWrapper, toolName, true);
            }
            ui.push();
            scrollToBottom();
        }));
    }

    private Div ensureToolsWrapper(Div parent) {
        Optional<Div> existing = parent.getChildren()
                .filter(c -> c.getElement().getClassList().contains(CSS_TOOLS_WRAPPER))
                .map(c -> (Div) c)
                .findAny();

        if (existing.isPresent()) return existing.get();

        Div wrapper = new Div();
        wrapper.addClassName(CSS_TOOLS_WRAPPER);
        parent.add(wrapper);
        return wrapper;
    }

    private void showToolRunning(Div wrapper, String toolName, boolean showLoading) {
        boolean hasTextRow = wrapper.getChildren()
                .anyMatch(c -> CSS_TOOLS_TEXT.equals(c.getClassName()));

        if (hasTextRow) return;

        Div textRow = new Div();
        textRow.addClassName(CSS_TOOLS_TEXT);

        if (showLoading) {
            Spinner spinner = new Spinner();
            textRow.add(spinner);
        }

        NativeLabel toolText = new NativeLabel(TOOL_RUNNING_PREFIX + toolName + TOOL_RUNNING_SUFFIX);
        textRow.add(toolText);

        wrapper.add(textRow);
    }

    private void hideSpinner(Div wrapper) {
        wrapper.getChildren()
                .filter(c -> CSS_TOOLS_TEXT.equals(c.getClassName()))
                .flatMap(Component::getChildren)
                .filter(Spinner.class::isInstance)
                .findAny()
                .ifPresent(spinner -> spinner.setVisible(false));
    }

    private void renderToolResponse(Div wrapper, JSONObject json) {
        String toolResponse = json.getString("toolResponse");
        showToolResponse(wrapper, toolResponse);
    }

    private void showToolResponse(Div wrapper, String toolResponse) {
        try {
            JsonNode jsonNode = objectMapper.readTree(toolResponse);
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);

            String escapedJson = StringEscapeUtils.escapeHtml4(prettyJson);

            String codeId = "json-" + UUID.randomUUID();

            String html = """
                        <pre class="json-output">
                          <code id="%s" class="language-json">%s</code>
                        </pre>
                    """.formatted(codeId, escapedJson);

            Html content = new Html(html);

            String title = "View JSON Output";
            Details details = new Details(title, content);
            details.setOpened(false);

            UI.getCurrent().getPage().executeJs(
                    """
                            const details = $0;
                            let done = false;
                            details.addEventListener('opened-changed', (e) => {
                              if (e.detail.value && !done) {
                                const el = details.querySelector('#%s');
                                if (el && window.hljs) { hljs.highlightElement(el); }
                                done = true;
                              }
                            });
                            """.formatted(codeId),
                    details.getElement()
            );

            wrapper.add(details);

        } catch (Exception ex) {
            log.error("Error in renderToolResponse", ex);
            wrapper.add(new NativeLabel("Error displaying tool response."));
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI.getCurrent().getPage().addStyleSheet(
                "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/atom-one-dark.min.css"
        );
        UI.getCurrent().getPage().addJavaScript(
                "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"
        );
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (streamSubscription != null && !streamSubscription.isDisposed()) {
            streamSubscription.dispose();
        }
    }

    private void scrollToBottom() {
        getUI().ifPresent(ui -> ui.getPage().executeJs("""
            const scroller = $0;
            if (scroller) {
              scroller.scrollTop = scroller.scrollHeight;
            }
        """, messageScroller.getElement()));
    }
}
