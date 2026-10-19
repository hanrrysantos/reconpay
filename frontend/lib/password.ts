/** Same rule as the API (`UserRequestDTO`): at least 8 characters, one uppercase letter and one digit. */
export const PASSWORD_RULES: { label: string; test: (value: string) => boolean }[] = [
  { label: '8 caracteres', test: value => value.length >= 8 },
  { label: 'Uma letra maiúscula', test: value => /[A-Z]/.test(value) },
  { label: 'Um número', test: value => /[0-9]/.test(value) },
]

export const PASSWORD_PATTERN = '(?=.*[A-Z])(?=.*[0-9]).{8,}'
