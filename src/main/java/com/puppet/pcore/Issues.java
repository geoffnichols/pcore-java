package com.puppet.pcore;

public interface Issues {
  String LEX_DOUBLE_COLON_NOT_FOLLOWED_BY_NAME = "DOUBLE_COLON_NOT_FOLLOWED_BY_NAME";
  String LEX_DIGIT_EXPECTED                    = "LEX_DIGIT_EXPECTED";
  String LEX_HEREDOC_EMPTY_TAG                 = "LEX_HEREDOC_EMPTY_TAG";
  String LEX_HEREDOC_ILLEGAL_ESCAPE            = "LEX_HEREDOC_ILLEGAL_ESCAPE";
  String LEX_HEREDOC_MULTIPLE_ESCAPE           = "LEX_HEREDOC_MULTIPLE_ESCAPE";
  String LEX_HEREDOC_MULTIPLE_SYNTAX           = "LEX_HEREDOC_MULTIPLE_SYNTAX";
  String LEX_HEREDOC_MULTIPLE_TAG              = "LEX_HEREDOC_MULTIPLE_TAG";
  String LEX_HEREDOC_DECL_UNTERMINATED         = "LEX_HEREDOC_DECL_UNTERMINATED";
  String LEX_HEREDOC_UNTERMINATED              = "LEX_HEREDOC_UNTERMINATED";
  String LEX_HEXDIGIT_EXPECTED                 = "LEX_HEXDIGIT_EXPECTED";
  String LEX_INVALID_NAME                      = "LEX_INVALID_NAME";
  String LEX_INVALID_OPERATOR                  = "LEX_INVALID_OPERATOR";
  String LEX_INVALID_TYPE_NAME                 = "LEX_INVALID_TYPE_NAME";
  String LEX_INVALID_VARIABLE_NAME             = "LEX_INVALID_VARIABLE_NAME";
  String LEX_MALFORMED_INTERPOLATION           = "LEX_MALFORMED_INTERPOLATION";
  String LEX_MALFORMED_UNICODE_ESCAPE          = "LEX_MALFORMED_UNICODE_ESCAPE";
  String LEX_OCTALDIGIT_EXPECTED               = "LEX_OCTALDIGIT_EXPECTED";
  String LEX_UNBALANCED_EPP_COMMENT            = "LEX_UNBALANCED_EPP_COMMENT";
  String LEX_UNEXPECTED_TOKEN                  = "LEX_UNEXPECTED_TOKEN";
  String LEX_UNTERMINATED_COMMENT              = "LEX_UNTERMINATED_COMMENT";
  String LEX_UNTERMINATED_STRING               = "LEX_UNTERMINATED_STRING";

  String PARSE_CLASS_NOT_VALID_HERE              = "PARSE_CLASS_NOT_VALID_HERE";
  String PARSE_ELSIF_IN_UNLESS                   = "PARSE_ELSIF_IN_UNLESS";
  String PARSE_EXPECTED_ATTRIBUTE_NAME           = "PARSE_EXPECTED_ATTRIBUTE_NAME";
  String PARSE_EXPECTED_CLASS_NAME               = "PARSE_EXPECTED_CLASS_NAME";
  String PARSE_EXPECTED_FARROW_AFTER_KEY         = "PARSE_EXPECTED_FARROW_AFTER_KEY";
  String PARSE_EXPECTED_NAME_OR_NUMBER_AFTER_DOT = "PARSE_EXPECTED_NAME_OR_NUMBER_AFTER_DOT";
  String PARSE_EXPECTED_NAME_AFTER_FUNCTION      = "PARSE_EXPECTED_NAME_AFTER_FUNCTION";
  String PARSE_EXPECTED_HOSTNAME                 = "PARSE_EXPECTED_HOSTNAME";
  String PARSE_EXPECTED_TITLE                    = "PARSE_EXPECTED_TITLE";
  String PARSE_EXPECTED_TOKEN                    = "PARSE_EXPECTED_TOKEN";
  String PARSE_EXPECTED_TYPE_NAME_AFTER_TYPE     = "PARSE_EXPECTED_TYPE_NAME_AFTER_TYPE";
  String PARSE_EXPECTED_VARIABLE                 = "PARSE_EXPECTED_VARIABLE";
  String PARSE_ILLEGAL_EPP_PARAMETERS            = "PARSE_ILLEGAL_EPP_PARAMETERS";
  String PARSE_INVALID_RESOURCE                  = "PARSE_INVALID_RESOURCE";
  String PARSE_INVALID_ATTRIBUTE                 = "PARSE_INVALID_ATTRIBUTE";
  String PARSE_INHERITS_MUST_BE_TYPE_NAME        = "PARSE_INHERITS_MUST_BE_TYPE_NAME";
  String PARSE_RESOURCE_WITHOUT_TITLE            = "PARSE_RESOURCE_WITHOUT_TITLE";
  String PARSE_QUOTED_NOT_VALID_NAME             = "PARSE_QUOTED_NOT_VALID_NAME";

  String VALIDATE_APPENDS_DELETES_NO_LONGER_SUPPORTED = "VALIDATE_APPENDS_DELETES_NO_LONGER_SUPPORTED";
  String VALIDATE_CROSS_SCOPE_ASSIGNMENT              = "VALIDATE_CROSS_SCOPE_ASSIGNMENT";
  String VALIDATE_IDEM_EXPRESSION_NOT_LAST            = "VALIDATE_IDEM_EXPRESSION_NOT_LAST";
  String VALIDATE_ILLEGAL_ASSIGNMENT_VIA_INDEX        = "VALIDATE_ILLEGAL_ASSIGNMENT_VIA_INDEX";
  String VALIDATE_ILLEGAL_ATTRIBUTE_APPEND            = "VALIDATE_ILLEGAL_ATTRIBUTE_APPEND";
  String VALIDATE_ILLEGAL_EXPRESSION                  = "VALIDATE_ILLEGAL_EXPRESSION";
  String VALIDATE_ILLEGAL_NUMERIC_ASSIGNMENT          = "VALIDATE_ILLEGAL_NUMERIC_ASSIGNMENT";
  String VALIDATE_NOT_RVALUE                          = "VALIDATE_NOT_RVALUE";
}
