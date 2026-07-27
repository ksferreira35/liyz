package br.com.kaiky.todo_list;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TodoListApplicationTests {

    @Test
    void shouldExposeApplicationEntryPoint() {
        assertThat(TodoListApplication.class).isNotNull();
    }
}
