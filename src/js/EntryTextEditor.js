import React, {Component} from 'react';
import {mdToDraftjs, draftjsToMd} from 'draftjs-md-converter';
import Draft, {RichUtils, EditorState, ContentState, convertToRaw, convertFromRaw} from 'draft-js';
import Editor, {createEditorStateWithText} from 'draft-js-plugins-editor'; // eslint-disable-line import/no-unresolved
import createMentionPlugin, {defaultSuggestionsFilter} from 'draft-js-mention-plugin'; // eslint-disable-line import/no-unresolved
import {fromJS} from 'immutable';
import editorStyles from './editorStyles.css';
import StyleControls from './style-controls';

const suggestionsFilter = (searchValue, suggestions) => {
    const value = searchValue.toLowerCase();
    const filteredSuggestions = suggestions.filter((suggestion) => {
        const name = suggestion.get("name").toLowerCase();
        const match = name.indexOf(value);
        return match > -1;
    });
    const size = filteredSuggestions.size < 15 ? filteredSuggestions.size : 15;
    return filteredSuggestions.setSize(size);
};

const myMdDict = {
    BOLD: '**',
    STRIKETHROUGH: '~~',
    CODE: '`'
};

export default class EntryTextEditor extends Component {
    state = {};

    handleKeyCommand = (command) => {
        const {editorState} = this.state;
        console.log("handleKeyCommand", command);
        const newState = RichUtils.handleKeyCommand(editorState, command);
        if (newState) {
            this.onChange(newState);
            return true;
        }
        return false;
    };

    _toggleInlineStyle(inlineStyle) {
        this.onChange(
            RichUtils.toggleInlineStyle(
                this.state.editorState,
                inlineStyle
            )
        );
    }

    _toggleBlockType(blockType) {
        this.onChange(
            RichUtils.toggleBlockType(
                this.state.editorState,
                blockType
            )
        );
    }

    onSearchChange = ({value}) => {
        let mentions = fromJS(this.props.mentions);
        this.setState({
            mentionSuggestions: defaultSuggestionsFilter(value, mentions),
        });
    };

    onSearchChange2 = ({value}) => {
        let hashtags = fromJS(this.props.hashtags);
        this.setState({
            hashtagSuggestions: defaultSuggestionsFilter(value, hashtags),
        });
    };

    onSearchChangeStories = ({value}) => {
        let stories = fromJS(this.props.stories);
        this.setState({
            storySuggestions: suggestionsFilter(value, stories),
        });
    };

    focus = () => {
        this.editor.focus();
    };

    onAddMention = () => {
        // get the mention object selected
    };

    onAddStory = (story) => {
    };

    constructor(props) {
        super(props);
        console.log(props);
        this.state.editorState = props.editorState;
        this.toggleInlineStyle = (style) => this._toggleInlineStyle(style);
        this.toggleBlockType = (type) => this._toggleBlockType(type);

        const hashtagPlugin = createMentionPlugin({
            mentionTrigger: "#",
        });

        const mentionPlugin = createMentionPlugin({
            mentionTrigger: "@",
        });

        const storyPlugin = createMentionPlugin({
            mentionTrigger: "$",
        });

        this.plugins = [hashtagPlugin, mentionPlugin, storyPlugin];
        this.HashtagSuggestions = hashtagPlugin.MentionSuggestions;
        this.MentionSuggestions = mentionPlugin.MentionSuggestions;
        this.StorySuggestions = storyPlugin.MentionSuggestions;

        this.state.mentionSuggestions = fromJS(props.mentions);
        this.state.hashtagSuggestions = fromJS(props.hashtags);
        this.state.storySuggestions = fromJS(props.stories);

        this.onChange = (newState) => {
            props.onChange(newState);
            const content = newState.getCurrentContent();
            const md = draftjsToMd(convertToRaw(content), myMdDict);
            console.log("md", md);
            this.setState({editorState: newState});
        };
    }

    render() {
        const HashtagSuggestions = this.HashtagSuggestions;
        const MentionSuggestions = this.MentionSuggestions;
        const StorySuggestions = this.StorySuggestions;
        const {editorState} = this.state;

        return (
            <div className="entry-text"
                 onClick={this.focus}>

                <StyleControls
                    editorState={editorState}
                    state={this}
                    onToggleInline={this.toggleInlineStyle}
                    onToggleBlockType={this.toggleBlockType}
                />
                <Editor
                    editorState={editorState}
                    onChange={this.onChange}
                    plugins={this.plugins}
                    handleKeyCommand={this.handleKeyCommand}
                    ref={(element) => {
                        this.editor = element;
                    }}
                />
                <MentionSuggestions
                    onSearchChange={this.onSearchChange}
                    suggestions={this.state.mentionSuggestions}
                    onAddMention={this.onAddMention}
                />
                <HashtagSuggestions
                    onSearchChange={this.onSearchChange2}
                    suggestions={this.state.hashtagSuggestions}
                    onAddMention={this.onAddMention}
                />
                <StorySuggestions
                    onSearchChange={this.onSearchChangeStories}
                    suggestions={this.state.storySuggestions}
                    onAddMention={this.onAddStory}
                />
            </div>
        );
    }
}